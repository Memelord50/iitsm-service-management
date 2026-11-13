package com.iitsm.sla;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sla_cycle")
public class SlaCycle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(updatable = false)
  public Long incidentId;

  @Column(updatable = false)
  public String kind;

  @Column(updatable = false)
  public Long policyId;

  @Column(updatable = false)
  public long policyVersion;

  @Column(updatable = false)
  public String priority;

  @Column(updatable = false)
  public long originalTargetSeconds;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)", updatable = false)
  public Instant startedAt;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant stoppedAt;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant pausedAt;

  public long pausedSeconds;
}
