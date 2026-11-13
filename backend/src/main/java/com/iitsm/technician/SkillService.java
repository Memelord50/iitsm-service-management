package com.iitsm.technician;

import com.iitsm.audit.Events;
import com.iitsm.common.*;
import com.iitsm.config.Settings;
import com.iitsm.incident.*;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class SkillService {
  private final Store db;
  private final Clock clock;
  private final Events events;

  public SkillService(Store d, Clock c, Events e) {
    db = d;
    clock = c;
    events = e;
  }

  public void verified(Incident i, Resolution resolution) {
    if (!db.list(SkillCredit.class, "where e.incidentId=?1", i.id).isEmpty()) return;
    var credit = new SkillCredit();
    credit.incidentId = i.id;
    credit.technicianId = resolution.technicianId;
    credit.categoryId = i.categoryId;
    credit.createdAt = clock.instant();
    db.save(credit);
    int count =
        db.list(
                SkillCredit.class,
                "where e.technicianId=?1 and e.categoryId=?2",
                credit.technicianId,
                i.categoryId)
            .size();
    int threshold = db.get(Settings.class, 1L).skillThreshold;
    if (count >= threshold
        && db.list(
                TechnicianSkill.class,
                "where e.technicianId=?1 and e.categoryId=?2",
                credit.technicianId,
                i.categoryId)
            .isEmpty()) {
      var skill = new TechnicianSkill();
      skill.technicianId = credit.technicianId;
      skill.categoryId = i.categoryId;
      skill.thresholdAtRecognition = threshold;
      skill.recognizedAt = clock.instant();
      db.save(skill);
      events.record(
          null,
          "SKILL_RECOGNIZED",
          "USER",
          credit.technicianId,
          "Category " + i.categoryId + " after " + count + " verified incidents");
      events.notify(credit.technicianId, i.id, "New recognized skill in category " + i.categoryId);
    }
  }
}
