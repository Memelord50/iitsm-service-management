package com.iitsm.assignment;

import com.iitsm.config.Settings;
import java.util.*;

public class PriorityWeighted implements AssignmentStrategy {
  public String name() {
    return "PRIORITY_WEIGHTED";
  }

  public Comparator<Candidate> order(String p, Settings s) {
    return Comparator.comparingLong(
        (Candidate c) ->
            c.workload() * s.loadWeight
                - (c.skilled()
                    ? (long) s.skillWeight * (com.iitsm.priority.PriorityEngine.rank(p) + 1)
                    : 0));
  }
}
