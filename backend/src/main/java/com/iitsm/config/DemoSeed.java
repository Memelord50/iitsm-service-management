package com.iitsm.config;

import com.iitsm.category.CatalogItem;
import com.iitsm.common.*;
import com.iitsm.sla.SlaPolicy;
import com.iitsm.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoSeed implements CommandLineRunner {
  private final Store db;
  private final PasswordEncoder passwords;
  private final boolean enabled;
  private final String password, thresholds;
  private final int skills, reopen;

  public DemoSeed(
      Store d,
      PasswordEncoder p,
      @Value("${app.seed}") boolean e,
      @Value("${app.seed-password}") String pw,
      @Value("${app.skill-threshold}") int sk,
      @Value("${app.warning-thresholds}") String th,
      @Value("${app.reopen-days}") int re) {
    db = d;
    passwords = p;
    enabled = e;
    password = pw;
    skills = sk;
    thresholds = th;
    reopen = re;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled || !db.list(User.class, "").isEmpty()) return;
    Problem.require(password.length() >= 12, "DEMO_PASSWORD must contain at least 12 characters");
    Settings s = db.lock(Settings.class, 1L);
    s.skillThreshold = skills;
    s.warningThresholds = thresholds;
    s.reopenDays = reopen;
    String[][] users = {
      {"Alex Morgan", "admin@iitsm.local", "ADMIN"},
      {"Jordan Lee", "technician@iitsm.local", "TECHNICIAN"},
      {"Casey Patel", "technician2@iitsm.local", "TECHNICIAN"},
      {"Sam Taylor", "employee@iitsm.local", "EMPLOYEE"},
      {"Robin Chen", "employee2@iitsm.local", "EMPLOYEE"}
    };
    for (var row : users) {
      var u = new User();
      u.name = row[0];
      u.email = row[1];
      u.role = row[2];
      u.passwordHash = passwords.encode(password);
      u.verified = true;
      db.save(u);
    }
    for (String name :
        new String[] {"Network & connectivity", "Workplace applications", "Devices & hardware"}) {
      var c = new CatalogItem();
      c.kind = "SERVICE";
      c.name = name;
      db.save(c);
      int level = 0;
      for (String priority : new String[] {"LOW", "MEDIUM", "HIGH", "CRITICAL"}) {
        var p = new SlaPolicy();
        p.serviceId = c.id;
        p.priority = priority;
        p.responseSeconds = new long[] {14400, 7200, 1800, 900}[level];
        p.resolutionSeconds = new long[] {172800, 86400, 14400, 3600}[level++];
        db.save(p);
      }
    }
    for (String name :
        new String[] {
          "Access & identity", "Connectivity", "Application fault", "Hardware failure"
        }) {
      var c = new CatalogItem();
      c.kind = "CATEGORY";
      c.name = name;
      db.save(c);
    }
  }
}
