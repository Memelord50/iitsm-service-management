package com.iitsm.assignment;

import com.iitsm.audit.Events;
import com.iitsm.common.*;
import com.iitsm.config.Settings;
import com.iitsm.incident.*;
import com.iitsm.technician.TechnicianSkill;
import com.iitsm.user.User;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AssignmentEngine {
  public record Selection(String strategy, String source, String reason, Long technicianId) {}

  private final Store db;
  private final StrategyAdvisor ai;
  private final Events events;
  private final Clock clock;
  private final Map<String, AssignmentStrategy> strategies;

  public AssignmentEngine(Store d, StrategyAdvisor a, Events e, Clock c) {
    db = d;
    ai = a;
    events = e;
    clock = c;
    strategies =
        Map.of(
            "LEAST_LOADED",
            new LeastLoaded(),
            "SKILL_FIRST",
            new SkillFirst(),
            "PRIORITY_WEIGHTED",
            new PriorityWeighted(),
            "ROUND_ROBIN",
            new RoundRobin());
  }

  public static boolean active(String status) {
    return !Set.of("RESOLVED", "CLOSED").contains(status);
  }

  public long workload(Long user) {
    return db.list(
            Incident.class,
            "where e.technicianId=?1 and e.status not in ('RESOLVED','CLOSED')",
            user)
        .size();
  }

  public Selection select(
      List<AssignmentStrategy.Candidate> candidates, String priority, Settings s, String context) {
    Set<String> enabled = new HashSet<>(Arrays.asList(s.enabledStrategies.split(",")));
    enabled.retainAll(strategies.keySet());
    String selected = "LEAST_LOADED", source = "FALLBACK", reason = "Deterministic fallback";
    try {
      String recommendation = ai.recommend(context, enabled);
      if (!enabled.contains(recommendation))
        throw new IllegalArgumentException("Invalid or disabled AI strategy");
      selected = recommendation;
      source = "AI";
      reason = "Validated AI recommendation";
    } catch (Exception ex) {
      reason =
          ex instanceof java.net.http.HttpTimeoutException
              ? "AI timeout"
              : ex.getMessage() == null ? "AI unavailable" : ex.getMessage();
    }
    Long id =
        strategies
            .get(selected)
            .choose(candidates, priority, s)
            .map(AssignmentStrategy.Candidate::id)
            .orElse(null);
    return new Selection(selected, source, reason, id);
  }

  public void auto(Incident i, Settings s, Long actor) {
    List<AssignmentStrategy.Candidate> candidates = new ArrayList<>();
    for (User u :
        db.list(
            User.class,
            "where e.role='TECHNICIAN' and e.active=true and e.verified=true and"
                + " e.available=true")) {
      boolean skilled =
          !db.list(
                  TechnicianSkill.class,
                  "where e.technicianId=?1 and e.categoryId=?2",
                  u.id,
                  i.categoryId)
              .isEmpty();
      var hist =
          db.page(
              AssignmentHistory.class, "where e.technicianId=?1 order by e.id desc", 0, 1, u.id);
      candidates.add(
          new AssignmentStrategy.Candidate(
              u.id, skilled, workload(u.id), hist.isEmpty() ? 0 : hist.getFirst().id));
    }
    var result =
        select(
            candidates,
            i.priority,
            s,
            "priority="
                + i.priority
                + "; categoryId="
                + i.categoryId
                + "; candidates="
                + candidates);
    if (result.technicianId() == null) {
      if (i.technicianId != null) {
        var unassigned = new AssignmentHistory();
        unassigned.incidentId = i.id;
        unassigned.previousTechnicianId = i.technicianId;
        unassigned.actorId = actor;
        unassigned.strategy = result.strategy();
        unassigned.selectionSource = result.source();
        unassigned.reason = "Previous technician ineligible; no eligible replacement";
        unassigned.createdAt = clock.instant();
        db.save(unassigned);
        events.notify(i.technicianId, i.id, i.reference + " returned to unassigned queue");
        i.technicianId = null;
      }
      events.incident(
          actor, "ASSIGNMENT_UNAVAILABLE", i, result.reason() + "; no eligible technician");
      events.admins(i.id, "Unassigned incident " + i.reference);
      return;
    }
    assign(i, result.technicianId(), actor, result.strategy(), result.source(), result.reason());
  }

  public void assign(
      Incident i, Long technician, Long actor, String strategy, String source, String reason) {
    User u = db.get(User.class, technician);
    Problem.require(
        u.role.equals("TECHNICIAN") && u.active && u.verified && u.available,
        "Technician is not eligible or available");
    Problem.require(!technician.equals(i.technicianId), "Already assigned to this technician");
    var h = new AssignmentHistory();
    h.incidentId = i.id;
    h.previousTechnicianId = i.technicianId;
    h.technicianId = technician;
    h.actorId = actor;
    h.strategy = strategy;
    h.selectionSource = source;
    h.reason = reason;
    h.createdAt = clock.instant();
    db.save(h);
    if (i.technicianId != null)
      events.notify(i.technicianId, i.id, i.reference + " reassigned: " + reason);
    i.technicianId = technician;
    if (i.status.equals("NEW")) i.status = "ASSIGNED";
    events.incident(
        actor,
        "ASSIGNED",
        i,
        "Technician " + technician + "; " + source + " / " + strategy + "; " + reason);
  }
}
