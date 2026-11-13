package com.iitsm.report;

import com.iitsm.audit.AuditLog;
import com.iitsm.auth.Access;
import com.iitsm.common.*;
import com.iitsm.notification.*;
import com.iitsm.search.SearchService;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Transactional
public class QueryController {
  private final Store db;
  private final Access access;
  private final SearchService search;
  private final ReportService reports;

  public QueryController(Store d, Access a, SearchService s, ReportService r) {
    db = d;
    access = a;
    search = s;
    reports = r;
  }

  @GetMapping("/incidents")
  public Object incidents(@RequestParam Map<String, String> f) {
    return search.search(f);
  }

  @GetMapping("/reports")
  public Object reports() {
    return reports.dashboard();
  }

  @GetMapping("/technicians")
  public Object technicians() {
    return reports.technicians();
  }

  @GetMapping("/admin/sla")
  public Object sla(@RequestParam(defaultValue = "0") int page) {
    return reports.slaDashboard(Math.max(0, page));
  }

  @GetMapping("/admin/sla-compliance")
  public Object compliance() {
    return reports.compliance();
  }

  @GetMapping("/admin/audit")
  public Object audit(@RequestParam(defaultValue = "0") int page) {
    access.admin();
    return db.page(AuditLog.class, "order by e.id desc", Math.max(0, page), 50).stream()
        .map(a -> Views.of(a, "id,actorId,action,entityType,entityId,details,createdAt"))
        .toList();
  }

  @GetMapping("/notifications")
  public Object notifications(@RequestParam(defaultValue = "0") int page) {
    return db
        .page(
            Notification.class,
            "where e.userId=?1 order by e.id desc",
            Math.max(0, page),
            50,
            access.me().id)
        .stream()
        .map(n -> Views.of(n, "id,incidentId,message,createdAt,readFlag,deliveryStatus"))
        .toList();
  }

  @PostMapping("/notifications/{id}/read")
  public Object read(@PathVariable Long id) {
    var n = db.get(Notification.class, id);
    if (!n.userId.equals(access.me().id)) throw new Problem(403, "Notification access denied");
    n.readFlag = true;
    return Map.of("message", "Marked read");
  }

  @GetMapping("/admin/delivery")
  public Object delivery() {
    access.admin();
    return Map.of(
        "notifications",
        db.page(Notification.class, "order by e.id desc", 0, 100).stream()
            .map(n -> Views.of(n, "id,userId,deliveryStatus,attempts,deliveryError,createdAt"))
            .toList(),
        "accountEmails",
        db.page(MailJob.class, "order by e.id desc", 0, 100).stream()
            .map(j -> Views.of(j, "id,recipient,subject,status,attempts,error,createdAt"))
            .toList());
  }
}
