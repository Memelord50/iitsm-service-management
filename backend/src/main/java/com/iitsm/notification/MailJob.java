package com.iitsm.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mail_job")
public class MailJob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String recipient;
  public String subject;

  @Column(columnDefinition = "TEXT")
  public String body;

  public String status = "PENDING";
  public int attempts;

  @Column(columnDefinition = "TEXT")
  public String error;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;
}
