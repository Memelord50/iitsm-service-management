package com.iitsm.auth;

import com.iitsm.user.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokens {
  private final byte[] secret;
  private final Clock clock;

  public JwtTokens(@Value("${app.jwt-secret}") String secret, Clock clock) {
    this.secret = Base64.getDecoder().decode(secret);
    if (this.secret.length < 32)
      throw new IllegalArgumentException("JWT_SECRET must be base64 of at least 32 random bytes");
    this.clock = clock;
  }

  public String issue(User u) {
    try {
      var claims =
          new JWTClaimsSet.Builder()
              .subject(u.id.toString())
              .issuer("i-itsm")
              .issueTime(Date.from(clock.instant()))
              .expirationTime(Date.from(clock.instant().plusSeconds(3600)))
              .claim("ver", u.tokenVersion)
              .jwtID(UUID.randomUUID().toString())
              .build();
      var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      token.sign(new MACSigner(secret));
      return token.serialize();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public JWTClaimsSet validate(String token) {
    try {
      var jwt = SignedJWT.parse(token);
      if (!jwt.getHeader().getAlgorithm().equals(JWSAlgorithm.HS256)
          || !jwt.verify(new MACVerifier(secret))) return null;
      var c = jwt.getJWTClaimsSet();
      return "i-itsm".equals(c.getIssuer())
              && c.getExpirationTime() != null
              && c.getExpirationTime().toInstant().isAfter(clock.instant())
          ? c
          : null;
    } catch (Exception e) {
      return null;
    }
  }
}
