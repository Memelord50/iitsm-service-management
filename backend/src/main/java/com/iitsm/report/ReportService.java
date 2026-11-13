package com.iitsm.report;

import com.iitsm.assignment.AssignmentEngine;
import com.iitsm.auth.*;
import com.iitsm.common.*;
import com.iitsm.feedback.Feedback;
import com.iitsm.incident.*;
import com.iitsm.sla.*;
import com.iitsm.technician.*;
import com.iitsm.user.User;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
  private final Store db;
  private final Access access;
  private final AssignmentEngine assignment;
  private final SlaService sla;

  public ReportService(Store d, Access a, AssignmentEngine as, SlaService sl) {
    db = d;
    access = a;
    assignment = as;
    sla = sl;
  }

  public Object dashboard() {
    var u = access.me();
    var incidents =
        db.list(
            Incident.class,
            u.role.equals("ADMIN")
                ? ""
                : u.role.equals("EMPLOYEE") ? "where e.reporterId=?1" : "where e.technicianId=?1",
            u.role.equals("ADMIN") ? new Object[] {} : new Object[] {u.id});
    Set<Long> ids = incidents.stream().map(i -> i.id).collect(Collectors.toSet());
    var cycles =
        db.list(SlaCycle.class, "").stream().filter(c -> ids.contains(c.incidentId)).toList();
    var signals = db.list(SlaSignal.class, "");
    var breached =
        signals.stream()
            .filter(s -> s.threshold == 100)
            .map(s -> s.cycleId)
            .collect(Collectors.toSet());
    long completed = cycles.stream().filter(c -> c.stoppedAt != null).count(),
        compliant =
            cycles.stream().filter(c -> c.stoppedAt != null && !breached.contains(c.id)).count();
    double average =
        cycles.stream()
            .filter(c -> c.kind.equals("RESOLUTION") && c.stoppedAt != null)
            .mapToLong(c -> SlaService.elapsed(c, c.stoppedAt))
            .average()
            .orElse(0);
    var m = new LinkedHashMap<String, Object>();
    m.put("total", incidents.size());
    m.put("active", incidents.stream().filter(i -> AssignmentEngine.active(i.status)).count());
    m.put(
        "resolved",
        incidents.stream().filter(i -> Set.of("RESOLVED", "CLOSED").contains(i.status)).count());
    m.put("breaches", cycles.stream().filter(c -> breached.contains(c.id)).count());
    m.put(
        "slaCompliance", completed == 0 ? null : Math.round(1000.0 * compliant / completed) / 10.0);
    m.put("averageResolutionSeconds", average);
    m.put("byStatus", group(incidents, i -> i.status));
    m.put("byPriority", group(incidents, i -> i.priority));
    m.put(
        "byCategory",
        group(incidents, i -> db.get(com.iitsm.category.CatalogItem.class, i.categoryId).name));
    m.put(
        "byService",
        group(incidents, i -> db.get(com.iitsm.category.CatalogItem.class, i.serviceId).name));
    m.put(
        "trends",
        group(incidents, i -> i.createdAt.atOffset(ZoneOffset.UTC).toLocalDate().toString()));
    var feedback =
        db.list(Feedback.class, "").stream().filter(f -> ids.contains(f.incidentId)).toList();
    m.put(
        "averageFeedback",
        feedback.isEmpty() ? null : feedback.stream().mapToInt(f -> f.rating).average().orElse(0));
    return m;
  }

  private Object group(List<Incident> list, Function<Incident, String> key) {
    return list.stream()
        .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting()))
        .entrySet()
        .stream()
        .map(e -> Map.of("name", e.getKey(), "value", e.getValue()))
        .toList();
  }

  public Object technicians() {
    var me = access.me();
    if (me.role.equals("EMPLOYEE")) throw new Problem(403, "Technician access required");
    var list =
        db.list(
            User.class,
            me.role.equals("ADMIN") ? "where e.role='TECHNICIAN'" : "where e.id=?1",
            me.role.equals("ADMIN") ? new Object[] {} : new Object[] {me.id});
    return list.stream()
        .map(
            u -> {
              var m = AuthService.view(u);
              m.put("workload", assignment.workload(u.id));
              m.put(
                  "verifiedResolutions",
                  db.list(SkillCredit.class, "where e.technicianId=?1", u.id).size());
              m.put(
                  "skills",
                  db.list(TechnicianSkill.class, "where e.technicianId=?1", u.id).stream()
                      .map(
                          s -> {
                            var v =
                                Views.of(s, "id,categoryId,thresholdAtRecognition,recognizedAt");
                            v.put(
                                "category",
                                db.get(com.iitsm.category.CatalogItem.class, s.categoryId).name);
                            return v;
                          })
                      .toList());
              return m;
            })
        .toList();
  }

  public Object slaDashboard(int page) {
    access.admin();
    return db.page(SlaCycle.class, "order by e.id desc", page, 50).stream()
        .map(
            c -> {
              var m = sla.view(c);
              var i = db.get(Incident.class, c.incidentId);
              m.put("incidentId", i.id);
              m.put("reference", i.reference);
              m.put("service", db.get(com.iitsm.category.CatalogItem.class, i.serviceId).name);
              return m;
            })
        .toList();
  }

  public Object compliance() {
    access.admin();
    var cycles = db.list(SlaCycle.class, "");
    var breached =
        db.list(SlaSignal.class, "where e.threshold=100").stream()
            .map(s -> s.cycleId)
            .collect(Collectors.toSet());
    Map<String, List<SlaCycle>> groups =
        cycles.stream()
            .collect(
                Collectors.groupingBy(
                    c ->
                        db.get(
                                    com.iitsm.category.CatalogItem.class,
                                    db.get(Incident.class, c.incidentId).serviceId)
                                .name
                            + " / "
                            + c.priority));
    return groups.entrySet().stream()
        .map(
            e -> {
              long stopped = e.getValue().stream().filter(c -> c.stoppedAt != null).count();
              long passed =
                  e.getValue().stream()
                      .filter(c -> c.stoppedAt != null && !breached.contains(c.id))
                      .count();
              var m = new HashMap<String, Object>();
              m.put("name", e.getKey());
              m.put("cycles", e.getValue().size());
              m.put("completed", stopped);
              m.put("compliance", stopped == 0 ? null : 100.0 * passed / stopped);
              return m;
            })
        .toList();
  }
}
