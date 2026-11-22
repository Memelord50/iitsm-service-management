package com.iitsm.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;
  private final Access access;

  public AuthController(AuthService service, Access access) {
    this.service = service;
    this.access = access;
  }

  public record Login(
      @Email @NotBlank String email,
      @NotBlank String password,
      @Pattern(regexp = "EMPLOYEE|TECHNICIAN|ADMIN") String role) {}

  public record Register(
      @NotBlank @Size(max = 100) String name,
      @Email @NotBlank @Size(max = 200) String email,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  public record EmailInput(@Email @NotBlank String email) {}

  public record Token(@NotBlank String token) {}

  public record Reset(@NotBlank String token, @NotBlank @Size(min = 12, max = 72) String password) {}

  @io.swagger.v3.oas.annotations.security.SecurityRequirements
  @PostMapping("/login")
  public Object login(@Valid @RequestBody Login d) {
    return service.login(d.email, d.password, d.role);
  }

  @io.swagger.v3.oas.annotations.security.SecurityRequirements
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Object register(@Valid @RequestBody Register d) {
    service.register(d.name, d.email, d.password);
    return Map.of("message", "Check your email for a verification link");
  }

  @io.swagger.v3.oas.annotations.security.SecurityRequirements
  @PostMapping("/forgot-password")
  public Object forgot(@Valid @RequestBody EmailInput d) {
    service.requestToken(d.email, "RESET");
    return Map.of(
        "message",
        "If that active account exists, a recovery or email verification link has been queued."
            + " Verify your email first, then request recovery again.");
  }

  @io.swagger.v3.oas.annotations.security.SecurityRequirements
  @PostMapping("/resend-verification")
  public Object resend(@Valid @RequestBody EmailInput d) {
    service.requestToken(d.email, "VERIFY");
    return Map.of("message", "If that account exists, a verification email has been queued");
  }

  @io.swagger.v3.oas.annotations.security.SecurityRequirements
  @PostMapping("/verify")
  public Object verify(@Valid @RequestBody Token d) {
    service.consume(d.token, "VERIFY", null);
    return Map.of("message", "Email verified. You can now sign in");
  }

  @io.swagger.v3.oas.annotations.security.SecurityRequirements
  @PostMapping("/reset-password")
  public Object reset(@Valid @RequestBody Reset d) {
    service.consume(d.token, "RESET", d.password);
    return Map.of("message", "Password updated");
  }

  @PostMapping("/change-password")
  public Object changePassword() {
    var user = access.me();
    service.requestToken(user.email, "RESET");
    return Map.of(
        "message",
        user.verified
            ? "A password-change link has been queued for your email."
            : "Verify your email using the link just queued, then request a password change"
                  + " again.");
  }

  @GetMapping("/me")
  public Object me() {
    return AuthService.view(access.me());
  }
}
