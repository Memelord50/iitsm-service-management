package com.iitsm.assignment;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assignment_history")
public class AssignmentHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long incidentId;
  public Long previousTechnicianId;
  public Long technicianId;
  public Long actorId;
  public String strategy;
  public String selectionSource;

  @Column(columnDefinition = "TEXT")
  public String reason;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant acknowledgedAt;
}
