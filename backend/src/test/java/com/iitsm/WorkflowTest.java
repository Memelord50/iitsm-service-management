package com.iitsm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iitsm.assignment.*;
import com.iitsm.attachment.*;
import com.iitsm.audit.*;
import com.iitsm.auth.*;
import com.iitsm.category.CatalogItem;
import com.iitsm.common.*;
import com.iitsm.config.Settings;
import com.iitsm.feedback.Feedback;
import com.iitsm.incident.*;
import com.iitsm.notification.*;
import com.iitsm.report.ReportService;
import com.iitsm.search.SearchService;
import com.iitsm.sla.*;
import com.iitsm.technician.*;
import com.iitsm.user.User;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class WorkflowTest {
  static org.springframework.context.ConfigurableApplicationContext context;
  static Clock testClock = mock(Clock.class);
  static StrategyAdvisor testAdvisor = mock(StrategyAdvisor.class);
  static JavaMailSender testMail = mock(JavaMailSender.class);
  org.springframework.transaction.PlatformTransactionManager tm;
  org.springframework.transaction.TransactionStatus tx;

  @org.springframework.boot.test.context.TestConfiguration
  static class Beans {
    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    Clock testClock() {
      return WorkflowTest.testClock;
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    StrategyAdvisor testAdvisor() {
      return WorkflowTest.testAdvisor;
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    JavaMailSender testMail() {
      return WorkflowTest.testMail;
    }
  }

  @BeforeAll
  static void start() {
    when(testClock.instant()).thenReturn(Instant.parse("2026-11-10T10:00:00Z"));
    when(testClock.getZone()).thenReturn(ZoneOffset.UTC);
    context =
        new org.springframework.boot.builder.SpringApplicationBuilder(
                Application.class, Beans.class)
            .profiles("test")
            .properties("server.port=0")
            .run();
  }

  @AfterAll
  static void stop() {
    if (context != null) context.close();
  }

  Store db;
  IncidentService incidents;
  SlaService sla;
  AssignmentEngine assignment;
  AuthService auth;
  SearchService search;
  ReportService reports;
  AttachmentService attachments;
  StrategyAdvisor advisor;
  JavaMailSender mail;
  Clock clock;
  User employee, other, technician, admin;
  Long service, category;
  Instant now = Instant.parse("2026-11-10T10:00:00Z");

  @BeforeEach
  void setup() throws Exception {
    db = context.getBean(Store.class);
    incidents = context.getBean(IncidentService.class);
    sla = context.getBean(SlaService.class);
    assignment = context.getBean(AssignmentEngine.class);
    auth = context.getBean(AuthService.class);
    search = context.getBean(SearchService.class);
    reports = context.getBean(ReportService.class);
    attachments = context.getBean(AttachmentService.class);
    advisor = testAdvisor;
    mail = testMail;
    clock = testClock;
    tm = context.getBean(org.springframework.transaction.PlatformTransactionManager.class);
    tx =
        tm.getTransaction(
            new org.springframework.transaction.support.DefaultTransactionDefinition());
    when(clock.instant()).thenReturn(now);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    doThrow(new IllegalStateException("AI key absent"))
        .when(advisor)
        .recommend(anyString(), anySet());
    employee = user("employee@iitsm.local");
    other = user("employee2@iitsm.local");
    technician = user("technician@iitsm.local");
    admin = user("admin@iitsm.local");
    service = db.list(CatalogItem.class, "where e.kind='SERVICE'").getFirst().id;
    category = db.list(CatalogItem.class, "where e.kind='CATEGORY'").getFirst().id;
    as(employee);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
    tm.rollback(tx);
  }

  User user(String email) {
    return db.list(User.class, "where e.email=?1", email).getFirst();
  }

  void as(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                u.id, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role))));
  }

  Incident create() {
    as(employee);
    var v =
        (Map<?, ?>)
            incidents.create(
                new IncidentController.Create(
                    "VPN cannot connect",
                    "Connection fails after sign in",
                    service,
                    category,
                    2,
                    2,
                    2,
                    4));
    return db.get(Incident.class, (Long) v.get("id"));
  }

  void tech(Incident i) {
    as(db.get(User.class, i.technicianId));
  }

  void acknowledge(Incident i) {
    tech(i);
    incidents.action(i.id, "ACKNOWLEDGE", "");
  }

  void resolve(Incident i) {
    acknowledge(i);
    incidents.action(i.id, "START", "");
    incidents.action(i.id, "RESOLVE", "Updated the client configuration");
  }

  void close(Incident i) {
    resolve(i);
    as(employee);
    incidents.action(i.id, "VERIFY", "");
  }

  @Test
  void adminCreatedStaffCanLoginWithoutFalsifyingVerification() {
    as(admin);
    var view =
        (Map<?, ?>)
            auth.createStaff(
                "New technician",
                "new.staff@example.com",
                "Staff-password-123!",
                "TECHNICIAN",
                context.getBean(Access.class));
    User u = db.get(User.class, (Long) view.get("id"));
    assertFalse(u.verified);
    assertTrue(u.adminCreated);
    assertFalse(view.containsKey("passwordHash"));
    var jwt = context.getBean(JwtTokens.class);
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + jwt.issue(u));
    SecurityContextHolder.clearContext();
    assertDoesNotThrow(
        () ->
            new JwtFilter(jwt, db)
                .doFilter(
                    request,
                    new org.springframework.mock.web.MockHttpServletResponse(),
                    (req, res) -> {
                      assertEquals(u.id, context.getBean(Access.class).me().id);
                    }));

    assertNotNull(auth.login(u.email, "Staff-password-123!", "TECHNICIAN").get("token"));
    assertThrows(Problem.class, () -> auth.login(u.email, "Staff-password-123!", "ADMIN"));
    var i = create();
    as(admin);
    assignment.assign(i, u.id, admin.id, "MANUAL", "ADMIN", "New staff assignment");
    assertEquals(u.id, i.technicianId);
    u.active = false;
    assertFalse(u.canSignIn());
    assertThrows(Problem.class, () -> auth.login(u.email, "Staff-password-123!"));
  }

  @Test
  void employeesCannotCreateStaff() {
    as(employee);
    assertThrows(
        Problem.class,
        () ->
            auth.createStaff(
                "Forbidden",
                "forbidden@example.com",
                "Staff-password-123!",
                "ADMIN",
                context.getBean(Access.class)));
  }

  @Test
  void unverifiedPasswordRecoveryRequestsVerificationFirst() {
    employee.verified = false;
    auth.requestToken(employee.email, "RESET");
    var tokens = db.list(AccountToken.class, "where e.userId=?1", employee.id);
    assertEquals(1, tokens.size());
    assertEquals("VERIFY", tokens.getFirst().kind);
  }

  @Test
  void messagesDoNotResumeSlaAndExplicitCompletionDoes() {
    var i = create();
    incidents.comment(i.id, "Additional details immediately after assignment", false);
    acknowledge(i);
    incidents.action(i.id, "START", "");
    incidents.action(i.id, "REQUEST_INFO", "Please provide details");
    var cycle = sla.cycles(i.id).getLast();
    as(employee);
    when(clock.instant()).thenReturn(now.plusSeconds(120));
    incidents.comment(i.id, "Here are the details", false);
    assertEquals("WAITING_FOR_EMPLOYEE", i.status);
    assertNotNull(cycle.pausedAt);
    incidents.action(i.id, "RESPOND", "");
    assertEquals("IN_PROGRESS", i.status);
    assertNull(cycle.pausedAt);
    assertEquals(120, cycle.pausedSeconds);
    assertThrows(Problem.class, () -> incidents.action(i.id, "RESPOND", ""));
  }

  @Test
  void closedChatIsReadOnlyAndUnrelatedUsersCannotRead() {
    var i = create();
    incidents.comment(i.id, "Keep this history", false);
    as(other);
    assertThrows(Problem.class, () -> incidents.detail(i.id));
    close(i);
    assertThrows(Problem.class, () -> incidents.comment(i.id, "Late message", false));
    assertEquals(1, ((List<?>) incidents.detail(i.id).get("comments")).size());
    as(admin);
    assertThrows(Problem.class, () -> incidents.comment(i.id, "Admin late message", false));
  }

  @Test
  void reassignmentTransfersChatAccessAndPreservesHistory() {
    var i = create();
    incidents.comment(i.id, "History for replacement", false);
    User previous = db.get(User.class, i.technicianId);
    User replacement =
        db.list(User.class, "where e.role='TECHNICIAN' and e.id<>?1", previous.id).getFirst();
    as(admin);
    assignment.assign(i, replacement.id, admin.id, "MANUAL", "ADMIN", "Transfer responsibility");
    as(replacement);
    assertEquals(1, ((List<?>) incidents.detail(i.id).get("comments")).size());
    as(previous);
    assertThrows(Problem.class, () -> incidents.detail(i.id));
    assertThrows(Problem.class, () -> incidents.comment(i.id, "Old assignee", false));
    as(admin);
    assertEquals(1, ((List<?>) incidents.detail(i.id).get("comments")).size());
  }

  @Test
  void unverifiedAccountCannotConsumePreviouslyIssuedResetToken() {
    auth.requestToken(employee.email, "RESET");
    var job =
        db.list(MailJob.class, "where e.recipient=?1 order by e.id desc", employee.email)
            .getFirst();
    String raw = job.body.substring(job.body.indexOf("&token=") + 7);
    employee.verified = false;
    assertThrows(Problem.class, () -> auth.consume(raw, "RESET", "A-new-test-password!"));
    assertFalse(
        db.list(AccountToken.class, "where e.tokenHash=?1", AuthService.hash(raw)).getFirst().used);
  }

  @Test
  void duplicateRegistrationExplainsExistingEmail() {
    Problem error =
        assertThrows(
            Problem.class,
            () -> auth.register("Duplicate", " EMPLOYEE@IITSM.LOCAL ", "another-password-123"));
    assertEquals(409, error.status);
    assertEquals("This email already exists.", error.getMessage());
  }

  @Test
  void successfulLogin() {
    assertNotNull(auth.login(employee.email, "Test-only-password-123!").get("token"));
  }

  @Test
  void invalidLoginDenied() {
    assertThrows(Problem.class, () -> auth.login(employee.email, "wrong"));
  }

  @Test
  void employeeCannotReadAnotherIncident() {
    var i = create();
    as(other);
    assertEquals(403, assertThrows(Problem.class, () -> incidents.detail(i.id)).status);
  }

  @Test
  void employeeCannotOverridePriority() {
    var i = create();
    assertEquals(
        403, assertThrows(Problem.class, () -> incidents.override(i.id, "LOW", "override")).status);
  }

  @Test
  void creationStartsBothClocksAtCreation() {
    var i = create();
    var cycles = sla.cycles(i.id);
    assertEquals(2, cycles.size());
    assertTrue(
        cycles.stream().allMatch(c -> c.startedAt.equals(i.createdAt) && c.stoppedAt == null));
    assertEquals("ASSIGNED", i.status);
    assertEquals(1, assignment.workload(i.technicianId));
  }

  @Test
  void assignmentAloneDoesNotStopResponse() {
    var i = create();
    assertNull(
        sla.cycles(i.id).stream()
            .filter(c -> c.kind.equals("RESPONSE"))
            .findFirst()
            .orElseThrow()
            .stoppedAt);
  }

  @Test
  void acknowledgementStopsOnlyResponse() {
    var i = create();
    when(clock.instant()).thenReturn(now.plusSeconds(30));
    acknowledge(i);
    var cycles = sla.cycles(i.id);
    assertEquals(now.plusSeconds(30), cycles.getFirst().stoppedAt);
    assertNull(cycles.getLast().stoppedAt);
    assertEquals(1, assignment.workload(i.technicianId));
  }

  @Test
  void startRequiresAcknowledgement() {
    var i = create();
    tech(i);
    assertThrows(Problem.class, () -> incidents.action(i.id, "START", ""));
  }

  @Test
  void completeWorkflowAuditNotificationsFeedback() {
    var i = create();
    Long assigned = i.technicianId;
    close(i);
    assertEquals("CLOSED", i.status);
    assertEquals(0, assignment.workload(assigned));
    incidents.feedback(i.id, 5, "Quick and helpful");
    assertEquals(1, db.list(Feedback.class, "where e.incidentId=?1", i.id).size());
    assertFalse(
        db.list(AuditLog.class, "where e.entityType='INCIDENT' and e.entityId=?1", i.id).isEmpty());
    assertFalse(db.list(Notification.class, "where e.incidentId=?1", i.id).isEmpty());
  }

  @Test
  void duplicateFeedbackDenied() {
    var i = create();
    close(i);
    incidents.feedback(i.id, 5, "Great");
    assertThrows(Problem.class, () -> incidents.feedback(i.id, 2, "Again"));
  }

  @Test
  void rejectionDoesNotCreditSkill() {
    var i = create();
    resolve(i);
    as(employee);
    incidents.action(i.id, "REJECT", "Still cannot connect");
    assertEquals("REOPENED", i.status);
    assertEquals(1, assignment.workload(i.technicianId));
    assertTrue(db.list(SkillCredit.class, "where e.incidentId=?1", i.id).isEmpty());
    assertEquals(3, sla.cycles(i.id).size());
  }

  @Test
  void reopenPreservesResponseAndAddsResolutionCycle() {
    var i = create();
    close(i);
    Instant responseStop = sla.cycles(i.id).getFirst().stoppedAt;
    when(clock.instant()).thenReturn(now.plusSeconds(3600));
    incidents.action(i.id, "REOPEN", "Issue returned");
    var cycles = sla.cycles(i.id);
    assertEquals(3, cycles.size());
    assertEquals(responseStop, cycles.getFirst().stoppedAt);
    assertEquals(now.plusSeconds(3600), cycles.getLast().startedAt);
    assertNotNull(cycles.get(1).stoppedAt);
    assertEquals(1, assignment.workload(i.technicianId));
  }

  @Test
  void reopenWindowEnforced() {
    var i = create();
    close(i);
    when(clock.instant()).thenReturn(now.plus(Duration.ofDays(91)));
    assertThrows(Problem.class, () -> incidents.action(i.id, "REOPEN", "Late return"));
  }

  @Test
  void verifiedCreditNotDuplicatedAcrossReopening() {
    var i = create();
    close(i);
    incidents.action(i.id, "REOPEN", "Returned");
    tech(i);
    incidents.action(i.id, "START", "");
    incidents.action(i.id, "RESOLVE", "Second repair");
    as(employee);
    incidents.action(i.id, "VERIFY", "");
    assertEquals(1, db.list(SkillCredit.class, "where e.incidentId=?1", i.id).size());
  }

  @Test
  void skillRecognizedAtConfiguredThreshold() {
    db.lock(Settings.class, 1L).skillThreshold = 1;
    var i = create();
    close(i);
    assertEquals(
        1,
        db.list(
                TechnicianSkill.class,
                "where e.technicianId=?1 and e.categoryId=?2",
                i.technicianId,
                i.categoryId)
            .size());
  }

  @Test
  void pauseAndResumePreserveElapsed() {
    var i = create();
    acknowledge(i);
    incidents.action(i.id, "START", "");
    when(clock.instant()).thenReturn(now.plusSeconds(60));
    incidents.action(i.id, "REQUEST_INFO", "Please provide error code");
    var c = sla.cycles(i.id).getLast();
    assertEquals(60, SlaService.elapsed(c, now.plusSeconds(600)));
    as(employee);
    when(clock.instant()).thenReturn(now.plusSeconds(660));
    incidents.action(i.id, "RESPOND", "Code 502");
    assertEquals(600, c.pausedSeconds);
    assertEquals(60, SlaService.elapsed(c, now.plusSeconds(660)));
    assertEquals(1, db.list(PauseRecord.class, "where e.cycleId=?1", c.id).size());
  }

  @Test
  void responseNeverPausesWithResolution() {
    var i = create();
    sla.pause(i);
    assertNull(sla.cycles(i.id).getFirst().pausedAt);
    assertNotNull(sla.cycles(i.id).getLast().pausedAt);
  }

  @Test
  void priorityRevisionPreservesSnapshotAndStart() {
    var i = create();
    var c = sla.cycles(i.id).getLast();
    long original = c.originalTargetSeconds;
    Instant start = c.startedAt;
    as(admin);
    incidents.override(i.id, "CRITICAL", "Entire department impacted");
    assertEquals(original, c.originalTargetSeconds);
    assertEquals(start, c.startedAt);
    var r = db.list(SlaRevision.class, "where e.cycleId=?1", c.id);
    assertEquals(1, r.size());
    assertEquals(original, r.getFirst().oldTargetSeconds);
    assertEquals(admin.id, r.getFirst().actorId);
    assertNotEquals(original, sla.target(c));
  }

  @Test
  void missingPolicyPreservesTargetAndNotifiesAdmin() {
    var i = create();
    var c = sla.cycles(i.id).getLast();
    for (var p :
        db.list(SlaPolicy.class, "where e.serviceId=?1 and e.priority='CRITICAL'", i.serviceId))
      p.active = false;
    as(admin);
    long target = sla.target(c);
    incidents.override(i.id, "CRITICAL", "Urgent");
    assertEquals(target, sla.target(c));
    assertTrue(db.list(SlaRevision.class, "where e.cycleId=?1", c.id).isEmpty());
    assertFalse(
        db.list(AuditLog.class, "where e.action='SLA_CONFIGURATION_ISSUE' and e.entityId=?1", i.id)
            .isEmpty());
  }

  @Test
  void deactivatedPolicyLeavesSnapshotIntact() {
    var i = create();
    var c = sla.cycles(i.id).getLast();
    long target = c.originalTargetSeconds;
    db.get(SlaPolicy.class, c.policyId).active = false;
    assertEquals(target, sla.target(c));
  }

  @Test
  void warningsAt50And80AndBreachAreIdempotent() {
    var i = create();
    var c = sla.cycles(i.id).getFirst();
    long target = sla.target(c);
    var s = db.get(Settings.class, 1L);
    when(clock.instant()).thenReturn(now.plusSeconds(target / 2));
    sla.detect(c, s);
    sla.detect(c, s);
    assertEquals(1, db.list(SlaSignal.class, "where e.cycleId=?1", c.id).size());
    when(clock.instant()).thenReturn(now.plusSeconds(target * 8 / 10));
    sla.detect(c, s);
    assertEquals(2, db.list(SlaSignal.class, "where e.cycleId=?1", c.id).size());
    when(clock.instant()).thenReturn(now.plusSeconds(target + 1));
    sla.detect(c, s);
    sla.detect(c, s);
    var signals = db.list(SlaSignal.class, "where e.cycleId=?1 order by e.threshold", c.id);
    assertEquals(3, signals.size());
    assertEquals(now.plusSeconds(target), signals.getLast().deadline);
    assertEquals(now.plusSeconds(target + 1), signals.getLast().detectedAt);
    assertNull(sla.cycles(i.id).getLast().stoppedAt);
  }

  @Test
  void reassignmentBeforeAcknowledgementTransfersWorkload() {
    var i = create();
    Long old = i.technicianId;
    var next = user("technician2@iitsm.local");
    as(admin);
    incidents.reassign(i.id, next.id, "Cover shift");
    assertEquals(0, assignment.workload(old));
    assertEquals(1, assignment.workload(next.id));
    assertNull(sla.cycles(i.id).getFirst().stoppedAt);
    acknowledge(i);
    assertNotNull(sla.cycles(i.id).getFirst().stoppedAt);
    assertEquals(2, db.list(AssignmentHistory.class, "where e.incidentId=?1", i.id).size());
  }

  @Test
  void reassignmentAfterAcknowledgementDoesNotRestartResponse() {
    var i = create();
    acknowledge(i);
    Instant stopped = sla.cycles(i.id).getFirst().stoppedAt;
    as(admin);
    incidents.reassign(i.id, user("technician2@iitsm.local").id, "Shift change");
    assertEquals(stopped, sla.cycles(i.id).getFirst().stoppedAt);
    assertNotNull(
        db.list(AssignmentHistory.class, "where e.incidentId=?1 order by e.id", i.id)
            .getFirst()
            .acknowledgedAt);
  }

  @Test
  void resolvedReassignmentDoesNotAddWorkload() {
    var i = create();
    resolve(i);
    as(admin);
    var next = user("technician2@iitsm.local");
    incidents.reassign(i.id, next.id, "Next owner");
    assertEquals(0, assignment.workload(next.id));
    as(employee);
    incidents.action(i.id, "REJECT", "Not fixed");
    assertEquals(1, assignment.workload(next.id));
  }

  @Test
  void unavailableTechnicianExcluded() {
    technician.available = false;
    var i = create();
    assertNotEquals(technician.id, i.technicianId);
  }

  @Test
  void attachmentAccessRestricted() throws Exception {
    var i = create();
    attachments.upload(
        i.id, new MockMultipartFile("file", "error.txt", "text/plain", "Error 502".getBytes()));
    var a = db.list(Attachment.class, "where e.incidentId=?1", i.id).getFirst();
    assertArrayEquals("Error 502".getBytes(), attachments.download(a.id));
    as(other);
    assertEquals(403, assertThrows(Problem.class, () -> attachments.download(a.id)).status);
  }

  @Test
  void employeeCannotReadInternalNotes() {
    var i = create();
    tech(i);
    incidents.comment(i.id, "Private investigation", true);
    as(employee);
    assertTrue(((List<?>) incidents.detail(i.id).get("comments")).isEmpty());
  }

  @Test
  void searchIsScopedAndFiltered() {
    var i = create();
    var result = (Map<?, ?>) search.search(Map.of("q", i.reference, "status", "ASSIGNED"));
    assertEquals(1L, result.get("total"));
    as(other);
    assertEquals(0L, ((Map<?, ?>) search.search(Map.of("q", i.reference))).get("total"));
  }

  @Test
  void reportCountsRealIncidents() {
    create();
    var r = (Map<?, ?>) reports.dashboard();
    assertEquals(1, r.get("total"));
    assertEquals(1L, r.get("active"));
  }

  @Test
  void verificationTokenIsSingleUse() {
    auth.register("New Employee", "new@example.com", "A-long-test-password!");
    User u = user("new@example.com");
    assertFalse(u.verified);
    var job = db.list(MailJob.class, "where e.recipient=?1", u.email).getFirst();
    String raw = job.body.substring(job.body.indexOf("&token=") + 7);
    auth.consume(raw, "VERIFY", null);
    assertTrue(u.verified);
    assertThrows(Problem.class, () -> auth.consume(raw, "VERIFY", null));
  }

  @Test
  void recoveryChangesPasswordAndInvalidatesTokens() {
    int version = employee.tokenVersion;
    auth.requestToken(employee.email, "RESET");
    var job =
        db.list(MailJob.class, "where e.recipient=?1 order by e.id desc", employee.email)
            .getFirst();
    String raw = job.body.substring(job.body.indexOf("&token=") + 7);
    auth.consume(raw, "RESET", "A-new-test-password!");
    assertEquals(version + 1, employee.tokenVersion);
    assertNotNull(auth.login(employee.email, "A-new-test-password!").get("token"));
    assertThrows(Problem.class, () -> auth.login(employee.email, "Test-only-password-123!"));
  }

  @Test
  void malformedAttachmentRejected() throws Exception {
    var i = create();
    assertThrows(
        Problem.class,
        () ->
            attachments.upload(
                i.id,
                new MockMultipartFile(
                    "file", "fake.pdf", "application/pdf", "not a PDF".getBytes())));
    assertTrue(db.list(Attachment.class, "where e.incidentId=?1", i.id).isEmpty());
  }

  @Test
  void httpRoleAuthorizationUsesJwtFilter() throws Exception {
    String token = (String) auth.login(employee.email, "Test-only-password-123!").get("token");
    String base = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port");
    var client = java.net.http.HttpClient.newHttpClient();
    var request =
        java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/admin/users"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
    var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(403, response.statusCode());
  }

  @Test
  void httpMissingAuthenticationRejected() throws Exception {
    String base = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port");
    var response =
        java.net.http.HttpClient.newHttpClient()
            .send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/incidents"))
                    .GET()
                    .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(401, response.statusCode());
  }

  @Test
  void warningThresholdsAreConfigurable() {
    var i = create();
    var c = sla.cycles(i.id).getFirst();
    var settings = db.get(Settings.class, 1L);
    settings.warningThresholds = "25,90";
    when(clock.instant()).thenReturn(now.plusSeconds(sla.target(c) / 4));
    sla.detect(c, settings);
    var signals = db.list(SlaSignal.class, "where e.cycleId=?1", c.id);
    assertEquals(1, signals.size());
    assertEquals(25, signals.getFirst().threshold);
  }

  @Test
  void feedbackBeforeClosureDenied() {
    var i = create();
    assertThrows(Problem.class, () -> incidents.feedback(i.id, 5, "too early"));
  }

  @Test
  void noEligibleTechnicianPreservesNewIncident() {
    for (var u : db.list(User.class, "where e.role='TECHNICIAN'")) u.available = false;
    var i = create();
    assertNull(i.technicianId);
    assertEquals("NEW", i.status);
    assertEquals(2, sla.cycles(i.id).size());
  }

  @Test
  void snapshotColumnsRemainImmutableAfterFlush() {
    var i = create();
    var c = sla.cycles(i.id).getLast();
    long original = c.originalTargetSeconds;
    Instant started = c.startedAt;
    Long policy = c.policyId;
    c.originalTargetSeconds = 1;
    c.startedAt = started.plusSeconds(60);
    c.policyId = 999L;
    db.flush();
    var em =
        org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(
            context.getBean(jakarta.persistence.EntityManagerFactory.class));
    em.clear();
    var persisted = db.get(SlaCycle.class, c.id);
    assertEquals(original, persisted.originalTargetSeconds);
    assertEquals(started, persisted.startedAt);
    assertEquals(policy, persisted.policyId);
  }

  @Test
  void reopenReassignsUnavailableOwnerWithoutLosingPreviousAssignment() {
    var i = create();
    close(i);
    Long old = i.technicianId;
    db.get(User.class, old).available = false;
    incidents.action(i.id, "REOPEN", "Issue returned while previous technician is unavailable");
    assertNotEquals(old, i.technicianId);
    assertEquals(0, assignment.workload(old));
    assertEquals(1, assignment.workload(i.technicianId));
    var history = db.list(AssignmentHistory.class, "where e.incidentId=?1 order by e.id", i.id);
    assertEquals(old, history.getLast().previousTechnicianId);
    assertNotNull(history.getFirst().acknowledgedAt);
    assertNull(history.getLast().acknowledgedAt);
  }

  @Test
  void priorityRevisionPreservesAccumulatedPauseAndDeadlineOrigin() {
    var i = create();
    acknowledge(i);
    incidents.action(i.id, "START", "");
    when(clock.instant()).thenReturn(now.plusSeconds(60));
    incidents.action(i.id, "REQUEST_INFO", "Supply error code");
    when(clock.instant()).thenReturn(now.plusSeconds(180));
    as(employee);
    incidents.action(i.id, "RESPOND", "502");
    var cycle = sla.cycles(i.id).getLast();
    long original = cycle.originalTargetSeconds;
    as(admin);
    incidents.override(i.id, "CRITICAL", "Wider impact");
    assertEquals(120, cycle.pausedSeconds);
    assertEquals(now, cycle.startedAt);
    assertEquals(original, cycle.originalTargetSeconds);
    assertEquals(now.plusSeconds(sla.target(cycle) + 120), sla.deadline(cycle, clock.instant()));
  }

  @Test
  void fallbackWorksWithEveryNormalStrategyDisabled() {
    db.lock(Settings.class, 1L).enabledStrategies = "";
    var i = create();
    assertNotNull(i.technicianId);
    var h = db.list(AssignmentHistory.class, "where e.incidentId=?1", i.id).getFirst();
    assertEquals("FALLBACK", h.selectionSource);
    assertEquals("LEAST_LOADED", h.strategy);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {-1, 0, 1})
  void revisionEvaluatesOriginalDeadline(long secondsFromDeadline) {
    var incident = create();
    var cycle = sla.cycles(incident.id).getFirst();
    Instant deadline = cycle.startedAt.plusSeconds(sla.target(cycle));
    when(clock.instant()).thenReturn(deadline.plusSeconds(secondsFromDeadline));
    as(admin);
    incidents.override(incident.id, "LOW", "Apply longer target");
    var breaches = db.list(SlaSignal.class, "where e.cycleId=?1 and e.threshold=100", cycle.id);
    assertEquals(secondsFromDeadline > 0 ? 1 : 0, breaches.size());
    if (!breaches.isEmpty()) {
      assertEquals(deadline, breaches.getFirst().deadline);
      assertEquals(deadline.plusSeconds(1), breaches.getFirst().detectedAt);
    }
  }

  @Test
  void httpRejectsBlankPasswordsOnEveryPasswordWritingRoute() throws Exception {
    String blank = " ".repeat(12);
    assertEquals(400, qaHttp("/auth/register", null, """
        {"name":"Test Employee","email":"blank@example.invalid","password":"%s"}
        """.formatted(blank)).statusCode());
    assertEquals(400, qaHttp("/auth/reset-password", null, """
        {"token":"validation-fixture","password":"%s"}
        """.formatted(blank)).statusCode());
    assertEquals(400, qaHttp("/admin/users", admin, """
        {"name":"Test Technician","email":"staff@example.invalid","password":"%s","role":"TECHNICIAN"}
        """.formatted(blank)).statusCode());
  }

  @Test
  void httpRejectsFractionalAffectedUsers() throws Exception {
    assertEquals(400, qaHttp("/incidents", employee, """
        {"title":"QA","description":"Fractional count","serviceId":%d,"categoryId":%d,
         "impact":2,"urgency":2,"criticality":2,"affectedUsers":1.7}
        """.formatted(service, category)).statusCode());
  }

  @Test
  void httpValidatesSearchDates() throws Exception {
    assertEquals(400, qaHttp("/incidents?from=bad", employee, null).statusCode());
    var reversed = qaHttp(
        "/incidents?from=2026-12-01T00:00:00Z&to=2026-01-01T00:00:00Z", employee, null);
    assertEquals(400, reversed.statusCode());
    assertTrue(reversed.body().contains("Start date must not be after the end date"));
    assertEquals(200, qaHttp("/incidents?from=", employee, null).statusCode());
    assertEquals(200, qaHttp(
        "/incidents?from=2026-01-01T00:00:00Z&before=2027-01-01T00:00:00Z&sort=priority",
        employee, null).statusCode());
  }

  private java.net.http.HttpResponse<String> qaHttp(String path, User user, String body)
      throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    var request = java.net.http.HttpRequest.newBuilder(
        java.net.URI.create("http://127.0.0.1:" + port + "/api" + path));
    if (user != null) {
      request.header("Authorization", "Bearer " + context.getBean(JwtTokens.class).issue(user));
    }
    if (body != null) {
      request.header("Content-Type", "application/json");
      request.POST(java.net.http.HttpRequest.BodyPublishers.ofString(body));
    }
    return java.net.http.HttpClient.newHttpClient()
        .send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
  }
}
