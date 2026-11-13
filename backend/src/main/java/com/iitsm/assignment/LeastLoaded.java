package com.iitsm.assignment;

import com.iitsm.config.Settings;
import java.util.*;

public class LeastLoaded implements AssignmentStrategy {
  public String name() {
    return "LEAST_LOADED";
  }

  public Comparator<Candidate> order(String p, Settings s) {
    return Comparator.comparingLong(Candidate::workload).thenComparing(c -> !c.skilled());
  }
}
