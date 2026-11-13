package com.iitsm.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long userId;
  public Long incidentId;

  @Column(columnDefinition = "TEXT")
  public String message;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;

  public boolean readFlag;
  public String deliveryStatus = "PENDING";
  public int attempts;

  @Column(columnDefinition = "TEXT")
  public String deliveryError;
}
