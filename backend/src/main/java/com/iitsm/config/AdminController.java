package com.iitsm.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdminController {
  private final AdminService service;

  public AdminController(AdminService s) {
    service = s;
  }

  public record Catalog(
      @NotBlank String kind, @NotBlank @Size(max = 100) String name, boolean active) {}

  public record UserUpdate(@NotBlank String role, boolean active, boolean available) {}

  public record Policy(
      @NotNull Long serviceId,
      @NotBlank String priority,
      @Min(1) @Max(31536000) long responseSeconds,
      @Min(1) @Max(31536000) long resolutionSeconds,
      boolean active) {}

  public record Config(
      @NotBlank String priorityStrategy,
      @Min(0) @Max(100) int impactWeight,
      @Min(0) @Max(100) int urgencyWeight,
      @Min(0) @Max(100) int criticalityWeight,
      @Min(0) @Max(100) int affectedWeight,
      @Min(1) int mediumCutoff,
      @Min(1) int highCutoff,
      @Min(1) int criticalCutoff,
      @NotBlank String matrix,
      @NotNull String enabledStrategies,
      @Min(1) @Max(10000) int skillWeight,
      @Min(1) @Max(10000) int loadWeight,
      @Min(1) @Max(10000) int skillThreshold,
      @NotBlank String warningThresholds,
      @Min(1) @Max(365) int reopenDays,
      boolean emailEnabled) {}

  @GetMapping("/catalog")
  public Object catalog() {
    return service.catalog();
  }

  @PostMapping("/admin/catalog")
  public Object catalogCreate(@Valid @RequestBody Catalog d) {
    return service.catalogSave(null, d);
  }

  @PutMapping("/admin/catalog/{id}")
  public Object catalogSave(@PathVariable Long id, @Valid @RequestBody Catalog d) {
    return service.catalogSave(id, d);
  }

  @GetMapping("/admin/users")
  public Object users(@RequestParam(defaultValue = "0") int page) {
    return service.users(Math.max(0, page));
  }

  @PutMapping("/admin/users/{id}")
  public Object user(@PathVariable Long id, @Valid @RequestBody UserUpdate d) {
    return service.user(id, d);
  }

  @GetMapping("/admin/settings")
  public Object settings() {
    return service.settings();
  }

  @PutMapping("/admin/settings")
  public Object config(@Valid @RequestBody Config d) {
    return service.settingsSave(d);
  }

  @GetMapping("/admin/sla-policies")
  public Object policies() {
    return service.policies();
  }

  @PostMapping("/admin/sla-policies")
  public Object policy(@Valid @RequestBody Policy d) {
    return service.policy(d);
  }
}
