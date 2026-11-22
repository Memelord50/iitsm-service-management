package com.iitsm.user;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;
  public String email;
  public String passwordHash;
  public String role;
  public boolean active = true;
  public boolean verified;
  public boolean adminCreated;

  public boolean canSignIn() {
    return active
        && (verified || (adminCreated && ("ADMIN".equals(role) || "TECHNICIAN".equals(role))));
  }

  public boolean available = true;
  public int tokenVersion;
}
