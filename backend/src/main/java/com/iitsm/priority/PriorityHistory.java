package com.iitsm.priority;

import jakarta.persistence.*;
import java.time.Instant;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "priority_history")
public class PriorityHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long incidentId;
  public String oldPriority;
  public String newPriority;
  public Long actorId;
  public long configVersion;

  @Column(columnDefinition = "TEXT")
  public String factors;

  @Column(columnDefinition = "TEXT")
  public String reason;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;
}
