package com.iitsm.audit;

import com.iitsm.common.Store;
import com.iitsm.incident.Incident;
import com.iitsm.user.User;
import java.time.Clock;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class Events {
  public record Notice(Long userId, Long incidentId, String message) {}

  private final Store db;
  private final Clock clock;
  private final ApplicationEventPublisher publisher;

  public Events(Store d, Clock c, ApplicationEventPublisher p) {
    db = d;
    clock = c;
    publisher = p;
  }

  public void record(Long actor, String action, String type, Long id, String details) {
    var a = new AuditLog();
    a.actorId = actor;
    a.action = action;
    a.entityType = type;
    a.entityId = id;
    a.details = details;
    a.createdAt = clock.instant();
    db.save(a);
  }

  public void incident(Long actor, String action, Incident i, String details) {
    record(actor, action, "INCIDENT", i.id, details);
    Set<Long> ids = new HashSet<>();
    ids.add(i.reporterId);
    if (i.technicianId != null) ids.add(i.technicianId);
    ids.forEach(
        id -> notify(id, i.id, i.reference + ": " + action.replace('_', ' ') + " — " + details));
  }

  public void notify(Long user, Long incident, String message) {
    publisher.publishEvent(new Notice(user, incident, message));
  }

  public void admins(Long incident, String message) {
    db.list(User.class, "where e.role='ADMIN' and e.active=true")
        .forEach(u -> notify(u.id, incident, message));
  }
}
