package com.iitsm.assignment;

import com.iitsm.config.Settings;
import java.util.*;

public interface AssignmentStrategy {
  record Candidate(long id, boolean skilled, long workload, long lastAssignment) {}

  String name();

  Comparator<Candidate> order(String priority, Settings settings);

  default Optional<Candidate> choose(List<Candidate> eligible, String priority, Settings s) {
    return eligible.stream().min(order(priority, s).thenComparingLong(Candidate::id));
  }
}
