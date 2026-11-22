package com.iitsm.notification;

import com.iitsm.audit.Events;
import com.iitsm.common.Store;
import com.iitsm.config.Settings;
import com.iitsm.user.User;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationService {
  private final Store db;
  private final Clock clock;
  private final JavaMailSender sender;
  private final String from;
  private final TransactionTemplate transactions;

  public NotificationService(
      Store d, Clock c, JavaMailSender s, PlatformTransactionManager manager, @Value("${app.mail-from}") String f) {
    db = d;
    clock = c;
    sender = s;
    from = f;
    transactions = new TransactionTemplate(manager);
  }

  @EventListener
  public void notice(Events.Notice n) {
    var v = new Notification();
    v.userId = n.userId();
    v.incidentId = n.incidentId();
    v.message = n.message();
    v.createdAt = clock.instant();
    v.deliveryStatus = db.get(Settings.class, 1L).emailEnabled ? "PENDING" : "DISABLED";
    db.save(v);
  }

  @Scheduled(fixedDelay = 30000, initialDelay = 30000)
  public void deliver() {
    var notificationIds = transactions.execute(status -> db.page(
        Notification.class,
        "where e.deliveryStatus='PENDING' or (e.deliveryStatus='FAILED' and e.attempts<3) order by e.id",
        0, 30).stream().map(n -> n.id).toList());
    for (Long id : notificationIds) {
      transactions.executeWithoutResult(status -> {
        var n = db.lock(Notification.class, id);
        if (!eligible(n.deliveryStatus, n.attempts)) return;
        n.attempts++;
        try {
          send(db.get(User.class, n.userId).email, "I-ITSM notification", n.message);
          n.deliveryStatus = "SENT";
          n.deliveryError = null;
        } catch (org.springframework.mail.MailException e) {
          n.deliveryStatus = "FAILED";
          n.deliveryError = "SMTP delivery failed";
        }
      });
    }

    var mailIds = transactions.execute(status -> db.page(
        MailJob.class,
        "where e.status='PENDING' or (e.status='FAILED' and e.attempts<3) order by e.id",
        0, 30).stream().map(j -> j.id).toList());
    for (Long id : mailIds) {
      transactions.executeWithoutResult(status -> {
        var j = db.lock(MailJob.class, id);
        if (!eligible(j.status, j.attempts)) return;
        j.attempts++;
        try {
          send(j.recipient, j.subject, j.body);
          j.status = "SENT";
          j.error = null;
          j.body = "Delivered";
        } catch (org.springframework.mail.MailException e) {
          j.status = "FAILED";
          j.error = "SMTP delivery failed";
        }
      });
    }
  }

  private static boolean eligible(String status, int attempts) {
    return "PENDING".equals(status) || ("FAILED".equals(status) && attempts < 3);
  }

  private void send(String to, String subject, String body) {
    var m = new SimpleMailMessage();
    m.setFrom(from);
    m.setTo(to);
    m.setSubject(subject);
    m.setText(body);
    sender.send(m);
  }
}
