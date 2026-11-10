package mx.taller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import mx.taller.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
  @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
    return http.csrf(c -> c.disable()) // API accepts only Bearer tokens, never cookies.
      .cors(Customizer.withDefaults())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .exceptionHandling(e -> e.authenticationEntryPoint((request, response, exception) -> { response.setStatus(HttpStatus.UNAUTHORIZED.value()); response.setContentType("application/json;charset=UTF-8"); response.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"Autenticación requerida\"}"); }).accessDeniedHandler((request, response, exception) -> { response.setStatus(HttpStatus.FORBIDDEN.value()); response.setContentType("application/json;charset=UTF-8"); response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"No tiene permisos para esta operación\"}"); }))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/test-reports", "/api/v1/test-reports/**").hasAnyRole("ADMINISTRADOR","MANAGER","AUDITOR")
        .requestMatchers(HttpMethod.POST, "/api/v1/test-reports").hasRole("ADMINISTRADOR")
        .requestMatchers(HttpMethod.GET, "/api/v1/status-catalog").hasAnyRole("ADMINISTRADOR", "MANAGER")
        .requestMatchers(HttpMethod.POST, "/api/v1/status-catalog").hasAnyRole("ADMINISTRADOR", "MANAGER")
        .requestMatchers(HttpMethod.PUT, "/api/v1/status-catalog/*").hasAnyRole("ADMINISTRADOR", "MANAGER")
        .requestMatchers(HttpMethod.GET, "/api/v1/clients").hasAnyRole("ADMINISTRADOR", "MANAGER", "RECEPCIONISTA", "SECRETARIA")
        .requestMatchers(HttpMethod.GET, "/api/v1/clients/me").hasRole("CUSTOMER")
        .requestMatchers(HttpMethod.GET, "/api/v1/clients/**").hasAnyRole("ADMINISTRADOR", "MANAGER", "RECEPCIONISTA", "SECRETARIA")
        .requestMatchers(HttpMethod.POST, "/api/v1/clients").hasAnyRole("ADMINISTRADOR", "MANAGER", "RECEPCIONISTA", "SECRETARIA")
        .requestMatchers(HttpMethod.PUT, "/api/v1/clients/**").hasAnyRole("ADMINISTRADOR", "MANAGER", "RECEPCIONISTA", "SECRETARIA")
        .requestMatchers(HttpMethod.POST, "/api/v1/clients/*/suspension").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "SECRETARIA")
        .requestMatchers(HttpMethod.POST, "/api/v1/clients/*/vehicles").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
        .requestMatchers(HttpMethod.GET, "/api/v1/vehicle-catalog/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
        .requestMatchers(HttpMethod.PUT, "/api/v1/vehicles/*").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
        .requestMatchers(HttpMethod.POST, "/api/v1/vehicles/*/deactivation").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
        .requestMatchers(HttpMethod.GET, "/api/v1/address-catalog/**").hasAnyRole("ADMINISTRADOR", "MANAGER", "RECEPCIONISTA", "SECRETARIA")
        .requestMatchers(HttpMethod.GET, "/api/v1/workshops/managers").hasAnyRole("ADMINISTRADOR", "MANAGER")
        .requestMatchers(HttpMethod.GET, "/api/v1/workshops/available").hasAnyRole("ADMINISTRADOR", "MANAGER", "RECEPCIONISTA", "SECRETARIA")
        .requestMatchers(HttpMethod.GET, "/api/v1/workshops/**").hasAnyRole("ADMINISTRADOR", "MANAGER")
        .requestMatchers(HttpMethod.POST, "/api/v1/workshops").hasAnyRole("ADMINISTRADOR", "MANAGER")
        .requestMatchers(HttpMethod.PUT, "/api/v1/workshops/**").hasAnyRole("ADMINISTRADOR", "MANAGER")
        .requestMatchers(HttpMethod.DELETE, "/api/v1/workshops/**").hasRole("ADMINISTRADOR")
        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").hasAnyRole("ADMINISTRADOR","MANAGER","TREASURY","MECHANIC_CHIEF","MECHANIC","RECEPCIONISTA","SECRETARIA","CUSTOMER","AUDITOR")
        .requestMatchers("/api/v1/audit/**").hasAnyRole("ADMINISTRADOR", "AUDITOR")
        .anyRequest().denyAll())
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
      .build();
  }
  @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String configuredOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(java.util.Arrays.stream(configuredOrigins.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    configuration.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration); return source;
  }
}
