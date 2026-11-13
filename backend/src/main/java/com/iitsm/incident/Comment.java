package com.iitsm.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident_comment")
public class Comment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long incidentId;
  public Long authorId;

  @Column(columnDefinition = "TEXT")
  public String body;

  public boolean internalNote;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;
}
