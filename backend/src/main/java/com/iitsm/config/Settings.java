package com.iitsm.config;

import jakarta.persistence.*;

@Entity
@Table(name = "settings")
public class Settings {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String priorityStrategy = "FORMULA";
  public int impactWeight = 3;
  public int urgencyWeight = 3;
  public int criticalityWeight = 2;
  public int affectedWeight = 1;
  public int mediumCutoff = 13;
  public int highCutoff = 20;
  public int criticalCutoff = 27;
  public String matrix = "LOW,LOW,MEDIUM,LOW,MEDIUM,HIGH,MEDIUM,HIGH,CRITICAL";
  public String enabledStrategies = "LEAST_LOADED,SKILL_FIRST,PRIORITY_WEIGHTED,ROUND_ROBIN";
  public int skillWeight = 100;
  public int loadWeight = 10;
  public int skillThreshold = 60;
  public String warningThresholds = "50,80";
  public int reopenDays = 90;
  public boolean emailEnabled;
  public long revision = 1;
}
