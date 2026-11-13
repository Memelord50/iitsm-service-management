package com.iitsm.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident")
public class Incident {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String reference;
  public String title;

  @Column(columnDefinition = "TEXT")
  public String description;

  public Long serviceId;
  public Long categoryId;
  public Long reporterId;
  public Long technicianId;
  public String status = "NEW";
  public String priority;
  public int impact;
  public int urgency;
  public int criticality;
  public int affectedUsers;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant closedAt;

  @Version public Long version;
}
