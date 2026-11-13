package com.iitsm.technician;

import jakarta.persistence.*;
import java.time.Instant;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "technician_skill")
public class TechnicianSkill {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long technicianId;
  public Long categoryId;
  public int thresholdAtRecognition;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant recognizedAt;
}
