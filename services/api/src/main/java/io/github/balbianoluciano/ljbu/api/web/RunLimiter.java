package io.github.balbianoluciano.ljbu.api.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Limits executions per address (docs/SECURITY.md §3, layer 1): a number per minute and one at a
 * time.
 */
public class RunLimiter {

  /** Addresses remembered at once; the oldest are forgotten first. */
  private static final int MAX_ADDRESSES = 100_000;

  private final int runsPerMinute;
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder()
          .maximumSize(MAX_ADDRESSES)
          .expireAfterAccess(Duration.ofMinutes(5))
          .build();
  private final Set<String> running = ConcurrentHashMap.newKeySet();

  public RunLimiter(int runsPerMinute) {
    this.runsPerMinute = runsPerMinute;
  }

  /** The right to run once; closing it lets the address run again. */
  public final class Permit implements AutoCloseable {
    private final String address;

    private Permit(String address) {
      this.address = address;
    }

    @Override
    public void close() {
      running.remove(address);
    }
  }

  /**
   * @throws RateLimitedException if the address already has an execution in progress or has used up
   *     its executions of the minute
   */
  public Permit acquire(String address) {
    if (!running.add(address)) {
      throw new RateLimitedException("run_in_progress", 1);
    }
    Bucket bucket =
        buckets.get(
            address,
            unused ->
                Bucket.builder()
                    .addLimit(
                        limit ->
                            limit
                                .capacity(runsPerMinute)
                                .refillGreedy(runsPerMinute, Duration.ofMinutes(1)))
                    .build());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      running.remove(address);
      long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
      throw new RateLimitedException("rate_limited", seconds);
    }
    return new Permit(address);
  }

  public static class RateLimitedException extends RuntimeException {
    private final String code;
    private final long retryAfterSeconds;

    public RateLimitedException(String code, long retryAfterSeconds) {
      this.code = code;
      this.retryAfterSeconds = retryAfterSeconds;
    }

    public String code() {
      return code;
    }

    public long retryAfterSeconds() {
      return retryAfterSeconds;
    }
  }
}
