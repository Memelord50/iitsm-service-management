package com.iitsm.auth;

import com.iitsm.audit.Events;
import com.iitsm.common.*;
import com.iitsm.notification.MailJob;
import com.iitsm.user.User;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
  private final com.iitsm.user.UserRepository usersRepository;
  private final Store db;
  private final PasswordEncoder passwords;
  private final JwtTokens jwt;
  private final Clock clock;
  private final String origin;
  private final Events events;

  public AuthService(
      com.iitsm.user.UserRepository usersRepository,
      Store db,
      PasswordEncoder passwords,
      JwtTokens jwt,
      Clock clock,
      @Value("${app.origin}") String origin,
      Events events) {
    this.usersRepository = usersRepository;
    this.db = db;
    this.passwords = passwords;
    this.jwt = jwt;
    this.clock = clock;
    this.origin = origin.split(",")[0];
    this.events = events;
  }

  public Map<String, Object> login(String email, String password) {
    var users = usersRepository.findByEmail(email.toLowerCase().trim()).stream().toList();
    if (users.isEmpty() || !passwords.matches(password, users.getFirst().passwordHash))
      throw new Problem(401, "Invalid email or password");
    var u = users.getFirst();
    if (!u.active || !u.verified) throw new Problem(403, "Account inactive or email not verified");
    return Map.of("token", jwt.issue(u), "user", view(u));
  }

  public void register(String name, String email, String password) {
    var u = new User();
    u.name = name.trim();
    u.email = email.toLowerCase().trim();
    u.passwordHash = passwords.encode(password);
    u.role = "EMPLOYEE";
    db.save(u);
    sendToken(u, "VERIFY");
    events.record(u.id, "REGISTER", "USER", u.id, "Employee registration");
  }

  public void requestToken(String email, String kind) {
    var users = usersRepository.findByEmail(email.toLowerCase().trim()).stream().toList();
    if (!users.isEmpty()) sendToken(users.getFirst(), kind);
  }

  private void sendToken(User u, String kind) {
    byte[] random = new byte[32];
    new SecureRandom().nextBytes(random);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    var t = new AccountToken();
    t.userId = u.id;
    t.kind = kind;
    t.tokenHash = hash(raw);
    t.expiresAt = clock.instant().plusSeconds(3600);
    db.save(t);
    var job = new MailJob();
    job.recipient = u.email;
    job.subject = kind.equals("VERIFY") ? "Verify your I-ITSM email" : "Reset your I-ITSM password";
    job.body = origin + "/?action=" + kind.toLowerCase() + "&token=" + raw;
    job.createdAt = clock.instant();
    db.save(job);
  }

  public void consume(String raw, String kind, String password) {
    var matches =
        db.list(AccountToken.class, "where e.tokenHash=?1 and e.kind=?2", hash(raw), kind);
    if (matches.isEmpty()) throw new Problem(422, "Invalid or expired link");
    var t = db.lock(AccountToken.class, matches.getFirst().id);
    if (t.used || !t.expiresAt.isAfter(clock.instant()))
      throw new Problem(422, "Invalid or expired link");
    User u = db.lock(User.class, t.userId);
    if (kind.equals("VERIFY")) u.verified = true;
    else {
      u.passwordHash = passwords.encode(password);
      u.tokenVersion++;
    }
    t.used = true;
    events.record(u.id, kind, "USER", u.id, "Account token consumed");
  }

  public static String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static Map<String, Object> view(User u) {
    return Views.of(u, "id,name,email,role,active,verified,available");
  }
}
