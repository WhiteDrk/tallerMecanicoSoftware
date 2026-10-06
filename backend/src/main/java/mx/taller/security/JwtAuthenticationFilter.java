package mx.taller.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  public JwtAuthenticationFilter(JwtService jwtService) { this.jwtService = jwtService; }
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
    try {
      Claims claims = jwtService.parse(header.substring(7));
      Object rawRoles = claims.get("roles");
      List<SimpleGrantedAuthority> authorities = rawRoles instanceof List<?> roles ? roles.stream().filter(String.class::isInstance).map(String.class::cast).map(SimpleGrantedAuthority::new).toList() : List.of();
      var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      chain.doFilter(request, response);
    } catch (JwtException | IllegalArgumentException exception) {
      SecurityContextHolder.clearContext();
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"code\":\"INVALID_TOKEN\",\"message\":\"Credenciales no válidas\"}");
    }
  }
}
