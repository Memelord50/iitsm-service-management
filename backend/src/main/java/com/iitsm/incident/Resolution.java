package com.iitsm.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resolution_record")
public class Resolution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long incidentId;
  public Long technicianId;

  @Column(columnDefinition = "TEXT")
  public String description;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant resolvedAt;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant verifiedAt;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant rejectedAt;

  @Column(columnDefinition = "TEXT")
  public String rejectionReason;
}
