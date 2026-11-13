package com.iitsm.attachment;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attachment_metadata")
public class Attachment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long incidentId;
  public Long uploaderId;
  public String filename;
  public String storageKey;
  public String contentType;
  public long sizeBytes;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant createdAt;

  public boolean deleted;
}
