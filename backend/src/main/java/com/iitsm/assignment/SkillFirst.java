package com.iitsm.assignment;

import com.iitsm.config.Settings;
import java.util.*;

public class SkillFirst implements AssignmentStrategy {
  public String name() {
    return "SKILL_FIRST";
  }

  public Comparator<Candidate> order(String p, Settings s) {
    return Comparator.comparing((Candidate c) -> !c.skilled())
        .thenComparingLong(Candidate::workload);
  }
}
