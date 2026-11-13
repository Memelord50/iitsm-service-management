package com.iitsm.auth;

import com.iitsm.common.*;
import com.iitsm.incident.Incident;
import com.iitsm.user.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Access {
  private final Store db;

  public Access(Store db) {
    this.db = db;
  }

  public User me() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !(a.getPrincipal() instanceof Long))
      throw new Problem(401, "Authentication required");
    return db.get(User.class, (Long) a.getPrincipal());
  }

  public void admin() {
    if (!me().role.equals("ADMIN")) throw new Problem(403, "Administrator access required");
  }

  public void incident(Incident i) {
    User u = me();
    if (!u.role.equals("ADMIN")
        && !u.id.equals(i.reporterId)
        && !(u.role.equals("TECHNICIAN") && u.id.equals(i.technicianId)))
      throw new Problem(403, "Incident access denied");
  }

  public void reporter(Incident i) {
    if (!me().id.equals(i.reporterId))
      throw new Problem(403, "Only the reporter can perform this action");
  }

  public void technician(Incident i) {
    User u = me();
    if (!u.role.equals("TECHNICIAN") || !u.id.equals(i.technicianId))
      throw new Problem(403, "Only the assigned technician can perform this action");
  }
}
