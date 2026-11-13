package com.iitsm.assignment;

import com.iitsm.config.Settings;
import java.util.*;

public class RoundRobin implements AssignmentStrategy {
  public String name() {
    return "ROUND_ROBIN";
  }

  public Comparator<Candidate> order(String p, Settings s) {
    return Comparator.comparingLong(Candidate::lastAssignment);
  }
}
