package com.iitsm;

import static org.junit.jupiter.api.Assertions.*;

import com.iitsm.assignment.ExternalLlmAdvisor;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ExternalProviderTest {
  @Test
  void externalProviderSendsContextAndParsesStrategy() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var request = new AtomicReference<String>();
    server.createContext(
        "/chat",
        exchange -> {
          request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] body =
              "{\"choices\":[{\"message\":{\"content\":\"SKILL_FIRST\"}}]}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      var advisor =
          new ExternalLlmAdvisor(
              "test-key",
              "http://127.0.0.1:" + server.getAddress().getPort() + "/chat",
              "test-model");
      assertEquals("SKILL_FIRST", advisor.recommend("priority=CRITICAL", Set.of("SKILL_FIRST")));
      assertTrue(request.get().contains("priority=CRITICAL"));
      assertTrue(request.get().contains("test-model"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void missingProviderKeyFailsClearly() {
    var advisor = new ExternalLlmAdvisor("", "https://example.invalid", "model");
    assertEquals(
        "AI key absent",
        assertThrows(
                IllegalStateException.class,
                () -> advisor.recommend("context", Set.of("SKILL_FIRST")))
            .getMessage());
  }
}
