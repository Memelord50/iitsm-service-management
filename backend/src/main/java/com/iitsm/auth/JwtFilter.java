package com.iitsm.auth;

import com.iitsm.common.Store;
import com.iitsm.user.User;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtFilter extends OncePerRequestFilter {
  private final JwtTokens tokens;
  private final Store db;

  public JwtFilter(JwtTokens t, Store d) {
    tokens = t;
    db = d;
  }

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain chain)
      throws ServletException, IOException {
    String h = q.getHeader("Authorization");
    if (h != null && h.startsWith("Bearer ")) {
      var c = tokens.validate(h.substring(7));
      if (c != null)
        try {
          User u = db.get(User.class, Long.valueOf(c.getSubject()));
          if (u.active && u.verified && u.tokenVersion == c.getIntegerClaim("ver"))
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        u.id, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role))));
        } catch (Exception ignored) {
        }
    }
    chain.doFilter(q, r);
  }
}
