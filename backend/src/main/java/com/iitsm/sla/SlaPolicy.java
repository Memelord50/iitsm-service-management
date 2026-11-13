package com.iitsm.sla;

import jakarta.persistence.*;

@Entity
@Table(name = "sla_policy")
public class SlaPolicy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long serviceId;
  public String priority;
  public long responseSeconds;
  public long resolutionSeconds;
  public boolean active = true;
  public long revision = 1;
}
