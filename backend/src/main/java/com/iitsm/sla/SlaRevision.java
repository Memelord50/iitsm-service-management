package com.iitsm.sla;

import jakarta.persistence.*;
import java.time.Instant;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "sla_revision")
public class SlaRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long cycleId;
  public Long policyId;
  public long policyVersion;
  public long oldTargetSeconds;
  public long newTargetSeconds;
  public String oldPriority;
  public String newPriority;
  public Long actorId;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;
}
