package com.iitsm.sla;

import com.iitsm.audit.Events;
import com.iitsm.common.*;
import com.iitsm.config.Settings;
import com.iitsm.incident.Incident;
import java.time.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaService {
  private final Store db;
  private final Clock clock;
  private final Events events;

  public SlaService(Store d, Clock c, Events e) {
    db = d;
    clock = c;
    events = e;
  }

  public Optional<SlaPolicy> policy(Incident i) {
    return db
        .page(
            SlaPolicy.class,
            "where e.serviceId=?1 and e.priority=?2 and e.active=true order by e.revision desc,e.id"
                + " desc",
            0,
            1,
            i.serviceId,
            i.priority)
        .stream()
        .findFirst();
  }

  public void initial(Incident i, Long actor) {
    SlaPolicy p =
        policy(i)
            .orElseThrow(
                () -> new Problem(422, "No active SLA policy for this service and priority"));
    create(i, p, "RESPONSE", i.createdAt, actor);
    create(i, p, "RESOLUTION", i.createdAt, actor);
  }

  public void reopen(Incident i, Long actor) {
    SlaPolicy p =
        policy(i).orElseThrow(() -> new Problem(422, "No active SLA policy for reopening"));
    create(i, p, "RESOLUTION", clock.instant(), actor);
  }

  private void create(Incident i, SlaPolicy p, String kind, Instant start, Long actor) {
    var c = new SlaCycle();
    c.incidentId = i.id;
    c.kind = kind;
    c.policyId = p.id;
    c.policyVersion = p.revision;
    c.priority = i.priority;
    c.originalTargetSeconds = kind.equals("RESPONSE") ? p.responseSeconds : p.resolutionSeconds;
    c.startedAt = start;
    db.save(c);
    events.record(
        actor,
        "SLA_CREATED",
        "INCIDENT",
        i.id,
        kind
            + " cycle "
            + c.id
            + " policy "
            + p.id
            + " version "
            + p.revision
            + " target seconds "
            + c.originalTargetSeconds);
  }

  public List<SlaCycle> cycles(Long incident) {
    return db.list(SlaCycle.class, "where e.incidentId=?1 order by e.id", incident);
  }

  public long target(SlaCycle c) {
    var r = db.page(SlaRevision.class, "where e.cycleId=?1 order by e.id desc", 0, 1, c.id);
    return r.isEmpty() ? c.originalTargetSeconds : r.getFirst().newTargetSeconds;
  }

  public static long elapsed(SlaCycle c, Instant now) {
    Instant end = c.stoppedAt != null ? c.stoppedAt : c.pausedAt != null ? c.pausedAt : now;
    return Math.max(0, Duration.between(c.startedAt, end).getSeconds() - c.pausedSeconds);
  }

  public Instant deadline(SlaCycle c, Instant now) {
    long currentPause =
        c.pausedAt == null ? 0 : Math.max(0, Duration.between(c.pausedAt, now).getSeconds());
    return c.startedAt.plusSeconds(target(c) + c.pausedSeconds + currentPause);
  }

  public void acknowledge(Incident i) {
    for (var c : cycles(i.id))
      if (c.kind.equals("RESPONSE") && c.stoppedAt == null) {
        detect(c, db.get(Settings.class, 1L));
        c.stoppedAt = clock.instant();
      }
  }

  public void pause(Incident i) {
    for (var c : cycles(i.id))
      if (c.kind.equals("RESOLUTION") && c.stoppedAt == null && c.pausedAt == null) {
        detect(c, db.get(Settings.class, 1L));
        c.pausedAt = clock.instant();
        var p = new PauseRecord();
        p.cycleId = c.id;
        p.startedAt = c.pausedAt;
        db.save(p);
      }
  }

  public void resume(Incident i) {
    for (var c : cycles(i.id))
      if (c.pausedAt != null) {
        c.pausedSeconds += Duration.between(c.pausedAt, clock.instant()).getSeconds();
        c.pausedAt = null;
        for (var p : db.list(PauseRecord.class, "where e.cycleId=?1 and e.endedAt is null", c.id))
          p.endedAt = clock.instant();
      }
  }

  public void resolve(Incident i) {
    for (var c : cycles(i.id))
      if (c.kind.equals("RESOLUTION") && c.stoppedAt == null) {
        detect(c, db.get(Settings.class, 1L));
        c.stoppedAt = clock.instant();
      }
  }

  public void revise(Incident i, String before, Long actor) {
    var p = policy(i);
    if (p.isEmpty()) {
      events.record(
          actor,
          "SLA_CONFIGURATION_ISSUE",
          "INCIDENT",
          i.id,
          "No active policy for " + i.priority + "; existing targets preserved");
      events.admins(i.id, "SLA configuration missing for " + i.reference);
      return;
    }
    for (var c : cycles(i.id))
      if (c.stoppedAt == null) {
        detect(c, db.get(Settings.class, 1L));
        var r = new SlaRevision();
        r.cycleId = c.id;
        r.policyId = p.get().id;
        r.policyVersion = p.get().revision;
        r.oldTargetSeconds = target(c);
        r.newTargetSeconds =
            c.kind.equals("RESPONSE") ? p.get().responseSeconds : p.get().resolutionSeconds;
        r.oldPriority = before;
        r.newPriority = i.priority;
        r.actorId = actor;
        r.createdAt = clock.instant();
        db.save(r);
        events.record(
            actor,
            "SLA_TARGET_REVISED",
            "INCIDENT",
            i.id,
            "Cycle "
                + c.id
                + ": "
                + r.oldTargetSeconds
                + " → "
                + r.newTargetSeconds
                + " seconds; "
                + before
                + " → "
                + i.priority);
      }
  }

  @Scheduled(fixedDelay = 15000, initialDelay = 15000)
  @Transactional
  public void check() {
    Settings s = db.lock(Settings.class, 1L);
    for (var c : db.list(SlaCycle.class, "where e.stoppedAt is null")) detect(c, s);
  }

  public void detect(SlaCycle c, Settings s) {
    Instant now = clock.instant();
    long elapsed = elapsed(c, now), target = target(c);
    for (String raw : s.warningThresholds.split(",")) {
      int t = Integer.parseInt(raw);
      if (elapsed * 100 >= target * t) signal(c, t, now);
    }
    if (elapsed > target) signal(c, 100, now);
  }

  private void signal(SlaCycle c, int threshold, Instant now) {
    if (!db.list(SlaSignal.class, "where e.cycleId=?1 and e.threshold=?2", c.id, threshold)
        .isEmpty()) return;
    var signal = new SlaSignal();
    signal.cycleId = c.id;
    signal.threshold = threshold;
    signal.deadline = deadline(c, now);
    signal.detectedAt = now;
    db.save(signal);
    Incident i = db.get(Incident.class, c.incidentId);
    String msg =
        c.kind
            + " SLA "
            + (threshold == 100 ? "breached" : threshold + "% warning")
            + "; cycle "
            + c.id;
    events.record(null, threshold == 100 ? "SLA_BREACH" : "SLA_WARNING", "INCIDENT", i.id, msg);
    if (i.technicianId != null) events.notify(i.technicianId, i.id, i.reference + ": " + msg);
    events.admins(i.id, i.reference + ": " + msg);
  }

  public Map<String, Object> view(SlaCycle c) {
    var m =
        Views.of(
            c,
            "id,kind,policyId,policyVersion,priority,originalTargetSeconds,startedAt,stoppedAt,pausedAt,pausedSeconds");
    m.put("targetSeconds", target(c));
    m.put("elapsedSeconds", elapsed(c, clock.instant()));
    m.put("deadline", deadline(c, clock.instant()));
    m.put(
        "revisions",
        db.list(SlaRevision.class, "where e.cycleId=?1 order by e.id", c.id).stream()
            .map(
                r ->
                    Views.of(
                        r,
                        "id,policyId,policyVersion,oldTargetSeconds,newTargetSeconds,oldPriority,newPriority,actorId,createdAt"))
            .toList());
    m.put(
        "pauses",
        db.list(PauseRecord.class, "where e.cycleId=?1 order by e.id", c.id).stream()
            .map(p -> Views.of(p, "id,startedAt,endedAt"))
            .toList());
    m.put(
        "signals",
        db.list(SlaSignal.class, "where e.cycleId=?1 order by e.id", c.id).stream()
            .map(s -> Views.of(s, "id,threshold,deadline,detectedAt"))
            .toList());
    return m;
  }
}
