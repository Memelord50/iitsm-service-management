package com.iitsm.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account_token")
public class AccountToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long userId;
  public String tokenHash;
  public String kind;

  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  @Column(columnDefinition = "datetime(6)")
  public Instant expiresAt;

  public boolean used;
}
