package com.iitsm.priority;

import com.iitsm.config.Settings;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PriorityEngine {
  public record Factors(int impact, int urgency, int criticality, int affectedUsers) {}

  public interface Strategy {
    String calculate(Factors f, Settings s);
  }

  public static class Formula implements Strategy {
    public String calculate(Factors f, Settings s) {
      int affected = f.affectedUsers() >= 100 ? 3 : f.affectedUsers() >= 10 ? 2 : 1;
      int score =
          f.impact() * s.impactWeight
              + f.urgency() * s.urgencyWeight
              + f.criticality() * s.criticalityWeight
              + affected * s.affectedWeight;
      return score >= s.criticalCutoff
          ? "CRITICAL"
          : score >= s.highCutoff ? "HIGH" : score >= s.mediumCutoff ? "MEDIUM" : "LOW";
    }
  }

  public static class Matrix implements Strategy {
    public String calculate(Factors f, Settings s) {
      String base = s.matrix.split(",")[(f.impact() - 1) * 3 + f.urgency() - 1];
      int rank = rank(base);
      if (f.criticality() == 3 || f.affectedUsers() >= 100) rank = Math.min(3, rank + 1);
      return new String[] {"LOW", "MEDIUM", "HIGH", "CRITICAL"}[rank];
    }
  }

  private final Map<String, Strategy> strategies =
      Map.of("FORMULA", new Formula(), "MATRIX", new Matrix());

  public String calculate(Factors f, Settings s) {
    return strategies.get(s.priorityStrategy).calculate(f, s);
  }

  public static int rank(String p) {
    return switch (p) {
      case "LOW" -> 0;
      case "MEDIUM" -> 1;
      case "HIGH" -> 2;
      case "CRITICAL" -> 3;
      default -> throw new IllegalArgumentException("Invalid priority");
    };
  }
}
