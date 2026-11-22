package com.iitsm.common;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ErrorsTest {
  @Test
  void concurrentEmailConflictUsesHelpfulMessage() {
    var error = new DataIntegrityViolationException("insert failed",
        new RuntimeException("Duplicate entry for key app_user.uq_app_user"));
    var response = new Errors().conflict(error);
    assertEquals(409, response.getStatusCode().value());
    assertEquals(Map.of("message", "This email already exists."), response.getBody());
  }

  @Test
  void unrelatedConstraintDoesNotClaimEmailExists() {
    var response = new Errors().conflict(new DataIntegrityViolationException("uq_feedback"));
    assertEquals(Map.of("message", "Conflicting or duplicate record"), response.getBody());
  }
}
