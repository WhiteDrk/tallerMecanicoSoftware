package mx.taller.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
  private static final int MAX_ATTEMPTS = 5;
  private static final long WINDOW_SECONDS = 900;
  private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
  public boolean allowed(String key) { Attempt current = attempts.get(key); return current == null || current.expiresAt().isBefore(Instant.now()) || current.count() < MAX_ATTEMPTS; }
  public void failure(String key) { attempts.compute(key, (ignored, current) -> current == null || current.expiresAt().isBefore(Instant.now()) ? new Attempt(1, Instant.now().plusSeconds(WINDOW_SECONDS)) : new Attempt(current.count() + 1, current.expiresAt())); }
  public void success(String key) { attempts.remove(key); }
  private record Attempt(int count, Instant expiresAt) { }
}
