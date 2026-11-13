package com.iitsm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iitsm.assignment.*;
import com.iitsm.audit.Events;
import com.iitsm.common.*;
import com.iitsm.config.Settings;
import com.iitsm.incident.*;
import com.iitsm.priority.*;
import com.iitsm.sla.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class DomainTest {
  Settings s = new Settings();

  @Test
  void formulaUsesAllFourFactors() {
    var p = new PriorityEngine();
    assertEquals("LOW", p.calculate(new PriorityEngine.Factors(1, 1, 1, 1), s));
    assertEquals("CRITICAL", p.calculate(new PriorityEngine.Factors(3, 3, 3, 100), s));
  }

  @Test
  void formulaConfigurationChangesResult() {
    s.criticalCutoff = 9;
    assertEquals(
        "CRITICAL", new PriorityEngine().calculate(new PriorityEngine.Factors(1, 1, 1, 1), s));
  }

  @Test
  void matrixUsesImpactAndUrgency() {
    s.priorityStrategy = "MATRIX";
    assertEquals("HIGH", new PriorityEngine().calculate(new PriorityEngine.Factors(2, 3, 1, 1), s));
  }

  @Test
  void matrixEscalatesCriticalBusiness() {
    s.priorityStrategy = "MATRIX";
    assertEquals(
        "MEDIUM", new PriorityEngine().calculate(new PriorityEngine.Factors(1, 1, 3, 1), s));
  }

  @Test
  void matrixEscalatesAffectedUsers() {
    s.priorityStrategy = "MATRIX";
    assertEquals(
        "MEDIUM", new PriorityEngine().calculate(new PriorityEngine.Factors(1, 1, 1, 100), s));
  }

  @Test
  void lifecycleAllowsInvestigation() {
    assertEquals("IN_PROGRESS", Lifecycle.next("ASSIGNED", "START"));
  }

  @Test
  void lifecycleRejectsPrematureClosure() {
    assertThrows(Problem.class, () -> Lifecycle.next("IN_PROGRESS", "VERIFY"));
  }

  @Test
  void lifecycleRequiresWaitingForResponse() {
    assertThrows(Problem.class, () -> Lifecycle.next("IN_PROGRESS", "RESPOND"));
  }

  @Test
  void resolvedVerificationCloses() {
    assertEquals("CLOSED", Lifecycle.next("RESOLVED", "VERIFY"));
  }

  @Test
  void rejectionReopens() {
    assertEquals("REOPENED", Lifecycle.next("RESOLVED", "REJECT"));
  }

  @Test
  void reopeningRequiresClosed() {
    assertThrows(Problem.class, () -> Lifecycle.next("ASSIGNED", "REOPEN"));
  }

  @Test
  void leastLoadedHasDeterministicTieBreak() {
    var c =
        List.of(
            new AssignmentStrategy.Candidate(2, false, 1, 1),
            new AssignmentStrategy.Candidate(1, false, 1, 2));
    assertEquals(1, new LeastLoaded().choose(c, "LOW", s).orElseThrow().id());
  }

  @Test
  void skillFirstDiffersFromLeastLoaded() {
    var c =
        List.of(
            new AssignmentStrategy.Candidate(1, false, 0, 1),
            new AssignmentStrategy.Candidate(2, true, 5, 2));
    assertEquals(1, new LeastLoaded().choose(c, "HIGH", s).orElseThrow().id());
    assertEquals(2, new SkillFirst().choose(c, "HIGH", s).orElseThrow().id());
  }

  @Test
  void roundRobinChoosesOldestAssignment() {
    var c =
        List.of(
            new AssignmentStrategy.Candidate(1, false, 0, 10),
            new AssignmentStrategy.Candidate(2, false, 4, 2));
    assertEquals(2, new RoundRobin().choose(c, "LOW", s).orElseThrow().id());
  }

  @Test
  void priorityRankingUsesSeverity() {
    var c =
        List.of(
            new AssignmentStrategy.Candidate(1, false, 0, 0),
            new AssignmentStrategy.Candidate(2, true, 15, 0));
    assertEquals(1, new PriorityWeighted().choose(c, "LOW", s).orElseThrow().id());
    assertEquals(2, new PriorityWeighted().choose(c, "CRITICAL", s).orElseThrow().id());
  }

  @Test
  void aiSelectsEnabledJavaStrategy() throws Exception {
    var ai = mock(StrategyAdvisor.class);
    when(ai.recommend(anyString(), anySet())).thenReturn("SKILL_FIRST");
    var e = new AssignmentEngine(mock(Store.class), ai, mock(Events.class), Clock.systemUTC());
    var r =
        e.select(List.of(new AssignmentStrategy.Candidate(1, true, 0, 0)), "HIGH", s, "context");
    assertEquals("AI", r.source());
    assertEquals("SKILL_FIRST", r.strategy());
  }

  @Test
  void missingKeyUsesFallback() throws Exception {
    var ai = mock(StrategyAdvisor.class);
    when(ai.recommend(anyString(), anySet())).thenThrow(new IllegalStateException("AI key absent"));
    var r =
        new AssignmentEngine(mock(Store.class), ai, mock(Events.class), Clock.systemUTC())
            .select(List.of(new AssignmentStrategy.Candidate(1, false, 0, 0)), "LOW", s, "context");
    assertEquals("FALLBACK", r.source());
    assertEquals(1, r.technicianId());
  }

  @Test
  void disabledFallbackStillAvailable() throws Exception {
    s.enabledStrategies = "SKILL_FIRST";
    var ai = mock(StrategyAdvisor.class);
    when(ai.recommend(anyString(), anySet())).thenReturn("UNKNOWN");
    var r =
        new AssignmentEngine(mock(Store.class), ai, mock(Events.class), Clock.systemUTC())
            .select(List.of(new AssignmentStrategy.Candidate(1, false, 0, 0)), "LOW", s, "context");
    assertEquals("LEAST_LOADED", r.strategy());
    assertEquals("FALLBACK", r.source());
  }

  @Test
  void disabledAiChoiceRejected() throws Exception {
    s.enabledStrategies = "ROUND_ROBIN";
    var ai = mock(StrategyAdvisor.class);
    when(ai.recommend(anyString(), anySet())).thenReturn("SKILL_FIRST");
    assertEquals(
        "FALLBACK",
        new AssignmentEngine(mock(Store.class), ai, mock(Events.class), Clock.systemUTC())
            .select(List.of(), "LOW", s, "context")
            .source());
  }

  @Test
  void noEligibleCandidateLeavesUnassigned() {
    assertTrue(new LeastLoaded().choose(List.of(), "HIGH", s).isEmpty());
  }

  @Test
  void workloadStateDefinition() {
    for (String state :
        List.of("NEW", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_EMPLOYEE", "REOPENED"))
      assertTrue(AssignmentEngine.active(state));
    assertFalse(AssignmentEngine.active("RESOLVED"));
    assertFalse(AssignmentEngine.active("CLOSED"));
  }

  @Test
  void pausedResolutionDoesNotAccrueTime() {
    var c = new SlaCycle();
    c.startedAt = Instant.parse("2026-01-01T00:00:00Z");
    c.pausedAt = c.startedAt.plusSeconds(60);
    assertEquals(60, SlaService.elapsed(c, c.startedAt.plusSeconds(600)));
  }

  @Test
  void resumedResolutionDeductsAccumulatedPause() {
    var c = new SlaCycle();
    c.startedAt = Instant.parse("2026-01-01T00:00:00Z");
    c.pausedSeconds = 120;
    assertEquals(180, SlaService.elapsed(c, c.startedAt.plusSeconds(300)));
  }

  @Test
  void stoppedSlaIsStable() {
    var c = new SlaCycle();
    c.startedAt = Instant.parse("2026-01-01T00:00:00Z");
    c.stoppedAt = c.startedAt.plusSeconds(42);
    assertEquals(42, SlaService.elapsed(c, c.startedAt.plusSeconds(5000)));
  }
}
