package com.iitsm.incident;

import com.iitsm.common.Problem;
import java.util.*;

/** State transition policy; actor checks are separately enforced by the application service. */
public final class Lifecycle {
  private static final Map<String, Set<String>> transitions =
      Map.of(
          "ACKNOWLEDGE",
          Set.of("ASSIGNED", "REOPENED", "IN_PROGRESS", "WAITING_FOR_EMPLOYEE"),
          "START",
          Set.of("ASSIGNED", "REOPENED"),
          "REQUEST_INFO",
          Set.of("IN_PROGRESS"),
          "RESPOND",
          Set.of("WAITING_FOR_EMPLOYEE"),
          "RESOLVE",
          Set.of("IN_PROGRESS"),
          "VERIFY",
          Set.of("RESOLVED"),
          "REJECT",
          Set.of("RESOLVED"),
          "REOPEN",
          Set.of("CLOSED"));

  public static String next(String state, String action) {
    Problem.require(
        transitions.containsKey(action) && transitions.get(action).contains(state),
        "Cannot " + action + " from " + state);
    return switch (action) {
      case "ACKNOWLEDGE" -> state;
      case "START", "RESPOND" -> "IN_PROGRESS";
      case "REQUEST_INFO" -> "WAITING_FOR_EMPLOYEE";
      case "RESOLVE" -> "RESOLVED";
      case "VERIFY" -> "CLOSED";
      case "REOPEN", "REJECT" -> "REOPENED";
      default -> throw new Problem(422, "Unknown action");
    };
  }
}
