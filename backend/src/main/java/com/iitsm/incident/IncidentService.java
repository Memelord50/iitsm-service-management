package com.iitsm.incident;

import com.iitsm.assignment.*;
import com.iitsm.audit.*;
import com.iitsm.auth.Access;
import com.iitsm.category.CatalogItem;
import com.iitsm.common.*;
import com.iitsm.config.Settings;
import com.iitsm.feedback.Feedback;
import com.iitsm.priority.*;
import com.iitsm.sla.*;
import com.iitsm.technician.SkillService;
import com.iitsm.user.User;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncidentService {
  private final Store db;
  private final Access access;
  private final Clock clock;
  private final Events events;
  private final PriorityEngine priority;
  private final AssignmentEngine assignment;
  private final SlaService sla;
  private final SkillService skills;

  public IncidentService(
      Store d,
      Access a,
      Clock c,
      Events e,
      PriorityEngine p,
      AssignmentEngine as,
      SlaService sl,
      SkillService sk) {
    db = d;
    access = a;
    clock = c;
    events = e;
    priority = p;
    assignment = as;
    sla = sl;
    skills = sk;
  }

  public Incident read(Long id) {
    var i = db.get(Incident.class, id);
    access.incident(i);
    return i;
  }

  public Object create(IncidentController.Create d) {
    User u = access.me();
    Problem.require(u.role.equals("EMPLOYEE"), "Only employees can report incidents");
    Settings s = db.lock(Settings.class, 1L);
    var service = db.get(CatalogItem.class, d.serviceId());
    var category = db.get(CatalogItem.class, d.categoryId());
    Problem.require(
        service.active
            && service.kind.equals("SERVICE")
            && category.active
            && category.kind.equals("CATEGORY"),
        "Choose an active service and category");
    var i = new Incident();
    i.reference = "INC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    i.title = d.title().trim();
    i.description = d.description().trim();
    i.serviceId = d.serviceId();
    i.categoryId = d.categoryId();
    i.reporterId = u.id;
    i.impact = d.impact();
    i.urgency = d.urgency();
    i.criticality = d.criticality();
    i.affectedUsers = d.affectedUsers();
    i.createdAt = clock.instant();
    i.priority =
        priority.calculate(
            new PriorityEngine.Factors(i.impact, i.urgency, i.criticality, i.affectedUsers), s);
    db.save(i);
    priorityHistory(i, null, u.id, s, "Automatic " + s.priorityStrategy);
    sla.initial(i, u.id);
    events.incident(u.id, "CREATED", i, i.title);
    assignment.auto(i, s, u.id);
    return detail(i.id);
  }

  private void priorityHistory(Incident i, String old, Long actor, Settings s, String reason) {
    var h = new PriorityHistory();
    h.incidentId = i.id;
    h.oldPriority = old;
    h.newPriority = i.priority;
    h.actorId = actor;
    h.configVersion = s.revision;
    h.factors =
        "impact="
            + i.impact
            + ", urgency="
            + i.urgency
            + ", criticality="
            + i.criticality
            + ", users="
            + i.affectedUsers;
    h.reason = reason;
    h.createdAt = clock.instant();
    db.save(h);
    events.record(
        actor,
        "PRIORITY_CALCULATED",
        "INCIDENT",
        i.id,
        h.factors + " → " + i.priority + "; configuration v" + s.revision);
  }

  public Object action(Long id, String action, String text) {
    Settings s = db.lock(Settings.class, 1L);
    Incident i = db.lock(Incident.class, id);
    access.incident(i);
    String next = Lifecycle.next(i.status, action);
    User u = access.me();
    if (Set.of("VERIFY", "REJECT", "REOPEN", "RESPOND").contains(action)) access.reporter(i);
    else access.technician(i);
    if (Set.of("REQUEST_INFO", "RESPOND", "RESOLVE", "REJECT", "REOPEN").contains(action))
      Problem.require(text != null && !text.isBlank(), "An explanation is required");
    switch (action) {
      case "ACKNOWLEDGE" -> {
        var histories =
            db.page(
                AssignmentHistory.class, "where e.incidentId=?1 order by e.id desc", 0, 1, i.id);
        Problem.require(
            !histories.isEmpty() && histories.getFirst().acknowledgedAt == null,
            "Current assignment already acknowledged");
        histories.getFirst().acknowledgedAt = clock.instant();
        sla.acknowledge(i);
      }
      case "START" -> {
        var h =
            db.page(
                AssignmentHistory.class, "where e.incidentId=?1 order by e.id desc", 0, 1, i.id);
        Problem.require(
            !h.isEmpty() && h.getFirst().acknowledgedAt != null,
            "Acknowledge your assignment first");
      }
      case "REQUEST_INFO" -> {
        sla.pause(i);
        commentInternal(i, text, false);
      }
      case "RESPOND" -> {
        commentInternal(i, text, false);
        sla.resume(i);
      }
      case "RESOLVE" -> {
        var h =
            db.page(
                AssignmentHistory.class, "where e.incidentId=?1 order by e.id desc", 0, 1, i.id);
        Problem.require(
            !h.isEmpty() && h.getFirst().acknowledgedAt != null,
            "Acknowledge your assignment first");
        var r = new Resolution();
        r.incidentId = i.id;
        r.technicianId = u.id;
        r.description = text;
        r.resolvedAt = clock.instant();
        db.save(r);
        sla.resolve(i);
      }
      case "VERIFY" -> {
        var r = latestResolution(i);
        r.verifiedAt = clock.instant();
        i.closedAt = clock.instant();
        skills.verified(i, r);
      }
      case "REJECT" -> {
        var r = latestResolution(i);
        r.rejectedAt = clock.instant();
        r.rejectionReason = text;
        sla.reopen(i, u.id);
      }
      case "REOPEN" -> {
        Problem.require(
            i.closedAt != null
                && !clock.instant().isAfter(i.closedAt.plus(Duration.ofDays(s.reopenDays))),
            "Reopening window expired");
        sla.reopen(i, u.id);
      }
      default -> throw new Problem(422, "Unknown action");
    }
    String before = i.status;
    i.status = next;
    if (Set.of("REJECT", "REOPEN").contains(action)) {
      if (i.technicianId == null || !eligible(i.technicianId)) {
        assignment.auto(i, s, u.id);
      }
    }
    events.incident(
        u.id,
        action,
        i,
        before + " → " + next + (text == null || text.isBlank() ? "" : "; " + text));
    return detail(id);
  }

  private boolean eligible(Long id) {
    var u = db.get(User.class, id);
    return u.active && u.verified && u.available && u.role.equals("TECHNICIAN");
  }

  private Resolution latestResolution(Incident i) {
    return db
        .page(Resolution.class, "where e.incidentId=?1 order by e.id desc", 0, 1, i.id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new Problem(422, "No resolution"));
  }

  public Object reassign(Long id, Long tech, String reason) {
    access.admin();
    Settings s = db.lock(Settings.class, 1L);
    var i = db.lock(Incident.class, id);
    assignment.assign(i, tech, access.me().id, "MANUAL", "ADMIN", reason);
    return detail(id);
  }

  public Object override(Long id, String value, String reason) {
    access.admin();
    Settings s = db.lock(Settings.class, 1L);
    var i = db.lock(Incident.class, id);
    PriorityEngine.rank(value);
    String before = i.priority;
    Problem.require(!before.equals(value), "Priority unchanged");
    i.priority = value;
    priorityHistory(i, before, access.me().id, s, reason);
    sla.revise(i, before, access.me().id);
    events.incident(
        access.me().id, "PRIORITY_OVERRIDDEN", i, before + " → " + value + "; " + reason);
    return detail(id);
  }

  private void commentInternal(Incident i, String body, boolean internal) {
    var c = new Comment();
    c.incidentId = i.id;
    c.authorId = access.me().id;
    c.body = body;
    c.internalNote = internal;
    c.createdAt = clock.instant();
    db.save(c);
  }

  public Object comment(Long id, String body, boolean internal) {
    var i = read(id);
    if (internal && access.me().role.equals("EMPLOYEE"))
      throw new Problem(403, "Internal notes are technician-only");
    commentInternal(i, body, internal);
    if (internal)
      events.record(access.me().id, "INVESTIGATION_NOTE", "INCIDENT", id, "Internal note added");
    else events.incident(access.me().id, "COMMENT", i, body);
    return detail(id);
  }

  public Object feedback(Long id, int rating, String comment) {
    db.lock(Settings.class, 1L);
    var i = db.lock(Incident.class, id);
    access.reporter(i);
    Problem.require(i.status.equals("CLOSED"), "Feedback requires verified closure");
    Problem.require(
        db.list(Feedback.class, "where e.incidentId=?1", id).isEmpty(),
        "Feedback already submitted");
    var f = new Feedback();
    f.incidentId = id;
    f.userId = access.me().id;
    f.rating = rating;
    f.comment = comment;
    f.createdAt = clock.instant();
    db.save(f);
    events.record(f.userId, "FEEDBACK", "INCIDENT", id, "Rating " + rating);
    return detail(id);
  }

  public Map<String, Object> summary(Incident i) {
    var m =
        Views.of(
            i,
            "id,reference,title,description,serviceId,categoryId,reporterId,technicianId,status,priority,impact,urgency,criticality,affectedUsers,createdAt,closedAt,version");
    m.put("service", db.get(CatalogItem.class, i.serviceId).name);
    m.put("category", db.get(CatalogItem.class, i.categoryId).name);
    m.put("reporter", db.get(User.class, i.reporterId).name);
    m.put("technician", i.technicianId == null ? null : db.get(User.class, i.technicianId).name);
    return m;
  }

  public Map<String, Object> detail(Long id) {
    var i = read(id);
    var m = summary(i);
    boolean employee = access.me().role.equals("EMPLOYEE");
    m.put(
        "comments",
        db.list(Comment.class, "where e.incidentId=?1 order by e.id", id).stream()
            .filter(c -> !employee || !c.internalNote)
            .map(
                c -> {
                  var v = Views.of(c, "id,authorId,body,internalNote,createdAt");
                  v.put("author", db.get(User.class, c.authorId).name);
                  return v;
                })
            .toList());
    m.put("sla", sla.cycles(id).stream().map(sla::view).toList());
    m.put(
        "assignments",
        db.list(AssignmentHistory.class, "where e.incidentId=?1 order by e.id", id).stream()
            .map(
                h ->
                    Views.of(
                        h,
                        "id,previousTechnicianId,technicianId,actorId,strategy,selectionSource,reason,createdAt,acknowledgedAt"))
            .toList());
    m.put(
        "resolutions",
        db.list(Resolution.class, "where e.incidentId=?1 order by e.id", id).stream()
            .map(
                r ->
                    Views.of(
                        r,
                        "id,technicianId,description,resolvedAt,verifiedAt,rejectedAt,rejectionReason"))
            .toList());
    m.put(
        "priorityHistory",
        db.list(PriorityHistory.class, "where e.incidentId=?1 order by e.id", id).stream()
            .map(
                h ->
                    Views.of(
                        h,
                        "id,oldPriority,newPriority,actorId,configVersion,factors,reason,createdAt"))
            .toList());
    m.put(
        "history",
        db
            .list(
                AuditLog.class, "where e.entityType='INCIDENT' and e.entityId=?1 order by e.id", id)
            .stream()
            .map(a -> Views.of(a, "id,actorId,action,details,createdAt"))
            .toList());
    m.put(
        "feedback",
        db.list(Feedback.class, "where e.incidentId=?1", id).stream()
            .map(f -> Views.of(f, "id,rating,comment,createdAt"))
            .toList());
    return m;
  }
}
