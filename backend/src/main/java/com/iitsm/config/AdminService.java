package com.iitsm.config;

import com.iitsm.assignment.AssignmentEngine;
import com.iitsm.audit.Events;
import com.iitsm.auth.*;
import com.iitsm.category.CatalogItem;
import com.iitsm.common.*;
import com.iitsm.priority.PriorityEngine;
import com.iitsm.sla.SlaPolicy;
import com.iitsm.user.User;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {
  private final Store db;
  private final Access access;
  private final Events events;
  private final AssignmentEngine assignment;

  public AdminService(Store d, Access a, Events e, AssignmentEngine as) {
    db = d;
    access = a;
    events = e;
    assignment = as;
  }

  public Object catalog() {
    return db.list(CatalogItem.class, "order by e.kind,e.name").stream()
        .map(c -> Views.of(c, "id,kind,name,active"))
        .toList();
  }

  public Object catalogSave(Long id, AdminController.Catalog d) {
    access.admin();
    db.lock(Settings.class, 1L);
    Problem.require(Set.of("SERVICE", "CATEGORY").contains(d.kind()), "Invalid catalog type");
    var c = id == null ? new CatalogItem() : db.get(CatalogItem.class, id);
    if (id != null) Problem.require(c.kind.equals(d.kind()), "Catalog type is immutable");
    c.kind = d.kind();
    c.name = d.name();
    c.active = d.active();
    if (id == null) db.save(c);
    events.record(
        access.me().id,
        "CATALOG_UPDATED",
        "CATALOG",
        c.id,
        c.kind + ": " + c.name + " active=" + c.active);
    return Views.of(c, "id,kind,name,active");
  }

  public Object users(int page) {
    access.admin();
    return db.page(User.class, "order by e.id", page, 50).stream().map(AuthService::view).toList();
  }

  public Object user(Long id, AdminController.UserUpdate d) {
    access.admin();
    db.lock(Settings.class, 1L);
    User u = db.lock(User.class, id);
    Problem.require(Set.of("ADMIN", "EMPLOYEE", "TECHNICIAN").contains(d.role()), "Invalid role");
    Problem.require(
        !u.id.equals(access.me().id) || d.active() && d.role().equals("ADMIN"),
        "Cannot deactivate or demote yourself");
    Problem.require(
        assignment.workload(id) == 0 || d.active() && d.role().equals("TECHNICIAN"),
        "Reassign active incidents before deactivating or changing role");
    String before = u.role + " active=" + u.active;
    u.role = d.role();
    u.active = d.active();
    u.available = d.available();
    u.tokenVersion++;
    events.record(
        access.me().id,
        "USER_UPDATED",
        "USER",
        id,
        before + " → " + u.role + " active=" + u.active);
    events.notify(id, null, "Account configuration changed by administrator");
    return AuthService.view(u);
  }

  public Object settings() {
    access.admin();
    return settingsView(db.get(Settings.class, 1L));
  }

  public Object settingsSave(AdminController.Config d) {
    access.admin();
    Settings s = db.lock(Settings.class, 1L);
    Problem.require(
        Set.of("FORMULA", "MATRIX").contains(d.priorityStrategy()), "Invalid priority strategy");
    Problem.require(
        d.mediumCutoff() < d.highCutoff() && d.highCutoff() < d.criticalCutoff(),
        "Cutoffs must increase");
    String[] matrix = d.matrix().split(",");
    Problem.require(matrix.length == 9, "Matrix must contain nine priorities");
    for (String value : matrix) PriorityEngine.rank(value);
    if (!d.enabledStrategies().isBlank())
      for (String name : d.enabledStrategies().split(","))
        Problem.require(
            Set.of("LEAST_LOADED", "SKILL_FIRST", "PRIORITY_WEIGHTED", "ROUND_ROBIN")
                .contains(name),
            "Unknown strategy");
    Set<Integer> thresholds = new HashSet<>();
    for (String value : d.warningThresholds().split(",")) {
      int t = Integer.parseInt(value);
      Problem.require(
          t > 0 && t < 100 && thresholds.add(t),
          "Warning thresholds must be distinct percentages between 1 and 99");
    }
    String before = settingsView(s).toString();
    s.priorityStrategy = d.priorityStrategy();
    s.impactWeight = d.impactWeight();
    s.urgencyWeight = d.urgencyWeight();
    s.criticalityWeight = d.criticalityWeight();
    s.affectedWeight = d.affectedWeight();
    s.mediumCutoff = d.mediumCutoff();
    s.highCutoff = d.highCutoff();
    s.criticalCutoff = d.criticalCutoff();
    s.matrix = d.matrix();
    s.enabledStrategies = d.enabledStrategies();
    s.skillWeight = d.skillWeight();
    s.loadWeight = d.loadWeight();
    s.skillThreshold = d.skillThreshold();
    s.warningThresholds = d.warningThresholds();
    s.reopenDays = d.reopenDays();
    s.emailEnabled = d.emailEnabled();
    s.revision++;
    events.record(
        access.me().id, "CONFIGURATION_UPDATED", "SETTINGS", 1L, before + " → " + settingsView(s));
    return settingsView(s);
  }

  public static Object settingsView(Settings s) {
    return Views.of(
        s,
        "id,priorityStrategy,impactWeight,urgencyWeight,criticalityWeight,affectedWeight,mediumCutoff,highCutoff,criticalCutoff,matrix,enabledStrategies,skillWeight,loadWeight,skillThreshold,warningThresholds,reopenDays,emailEnabled,revision");
  }

  public Object policies() {
    access.admin();
    return db.list(SlaPolicy.class, "order by e.serviceId,e.priority,e.id desc").stream()
        .map(
            p ->
                Views.of(
                    p, "id,serviceId,priority,responseSeconds,resolutionSeconds,active,revision"))
        .toList();
  }

  public Object policy(AdminController.Policy d) {
    access.admin();
    db.lock(Settings.class, 1L);
    PriorityEngine.rank(d.priority());
    Problem.require(
        db.get(CatalogItem.class, d.serviceId()).kind.equals("SERVICE"), "Not a service");
    var old =
        db.list(
            SlaPolicy.class,
            "where e.serviceId=?1 and e.priority=?2 order by e.revision desc",
            d.serviceId(),
            d.priority());
    long revision = old.isEmpty() ? 1 : old.getFirst().revision + 1;
    for (var p : old) p.active = false;
    var p = new SlaPolicy();
    p.serviceId = d.serviceId();
    p.priority = d.priority();
    p.responseSeconds = d.responseSeconds();
    p.resolutionSeconds = d.resolutionSeconds();
    p.active = d.active();
    p.revision = revision;
    db.save(p);
    events.record(
        access.me().id,
        "SLA_POLICY_UPDATED",
        "SLA_POLICY",
        p.id,
        "Version "
            + revision
            + " response="
            + p.responseSeconds
            + " resolution="
            + p.resolutionSeconds
            + " active="
            + p.active);
    return Views.of(p, "id,serviceId,priority,responseSeconds,resolutionSeconds,active,revision");
  }
}
