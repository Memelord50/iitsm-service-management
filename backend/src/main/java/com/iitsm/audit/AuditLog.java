package com.iitsm.audit;

import jakarta.persistence.*;
import java.time.Instant;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "audit_log")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long actorId;
  public String action;
  public String entityType;
  public Long entityId;

  @Column(columnDefinition = "TEXT")
  public String details;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;
}
