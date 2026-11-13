package com.iitsm.incident;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
  private final IncidentService service;

  public IncidentController(IncidentService s) {
    service = s;
  }

  public record Create(
      @NotBlank @Size(max = 160) String title,
      @NotBlank @Size(max = 20000) String description,
      @NotNull Long serviceId,
      @NotNull Long categoryId,
      @Min(1) @Max(3) int impact,
      @Min(1) @Max(3) int urgency,
      @Min(1) @Max(3) int criticality,
      @Min(1) @Max(10000000) int affectedUsers) {}

  public record Action(@NotBlank String action, @Size(max = 20000) String text) {}

  public record CommentInput(@NotBlank @Size(max = 20000) String body, boolean internalNote) {}

  public record Reassign(@NotNull Long technicianId, @NotBlank @Size(max = 1000) String reason) {}

  public record Override(@NotBlank String priority, @NotBlank @Size(max = 1000) String reason) {}

  public record FeedbackInput(@Min(1) @Max(5) int rating, @Size(max = 5000) String comment) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Object create(@Valid @RequestBody Create d) {
    return service.create(d);
  }

  @GetMapping("/{id}")
  public Object detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PostMapping("/{id}/actions")
  public Object action(@PathVariable Long id, @Valid @RequestBody Action d) {
    return service.action(id, d.action, d.text);
  }

  @PostMapping("/{id}/comments")
  public Object comment(@PathVariable Long id, @Valid @RequestBody CommentInput d) {
    return service.comment(id, d.body, d.internalNote);
  }

  @PostMapping("/{id}/reassign")
  public Object reassign(@PathVariable Long id, @Valid @RequestBody Reassign d) {
    return service.reassign(id, d.technicianId, d.reason);
  }

  @PostMapping("/{id}/priority")
  public Object priority(@PathVariable Long id, @Valid @RequestBody Override d) {
    return service.override(id, d.priority, d.reason);
  }

  @PostMapping("/{id}/feedback")
  public Object feedback(@PathVariable Long id, @Valid @RequestBody FeedbackInput d) {
    return service.feedback(id, d.rating, d.comment);
  }
}
