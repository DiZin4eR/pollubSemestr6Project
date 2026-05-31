package com.ecodatahub.gus.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Component
public class GusRateLimitRetryPolicy {

    private static final int MAX_RETRIES_AFTER_RATE_LIMIT = 3;
    private static final Duration DEFAULT_RATE_LIMIT_WAIT = Duration.ofMinutes(15);

    public <T> T execute(GusApiRequest<T> request) {
        for (int attempt = 1; attempt <= MAX_RETRIES_AFTER_RATE_LIMIT; attempt++) {
            try {
                return request.execute();
            } catch (WebClientResponseException.TooManyRequests exception) {
                Duration waitTime = getRateLimitWaitTime(exception.getHeaders());

                log.warn(
                        "GUS API returned 429. Waiting {} seconds before retry {}/{}.",
                        waitTime.toSeconds(),
                        attempt,
                        MAX_RETRIES_AFTER_RATE_LIMIT
                );

                sleep(waitTime);
            }
        }

        return request.execute();
    }

    private Duration getRateLimitWaitTime(HttpHeaders headers) {
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);

        if (retryAfter != null) {
            try {
                return Duration.ofSeconds(Long.parseLong(retryAfter));
            } catch (NumberFormatException ignored) {
                log.warn("GUS API returned unsupported Retry-After value: {}", retryAfter);
            }
        }

        String reset = headers.getFirst("X-Rate-Limit-Reset");

        if (reset != null) {
            try {
                Duration waitTime = Duration.between(
                        OffsetDateTime.now(),
                        OffsetDateTime.parse(reset)
                );

                if (!waitTime.isNegative() && !waitTime.isZero()) {
                    return waitTime.plusSeconds(1);
                }
            } catch (RuntimeException ignored) {
                log.warn("GUS API returned unsupported X-Rate-Limit-Reset value: {}", reset);
            }
        }

        return DEFAULT_RATE_LIMIT_WAIT;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Thread interrupted while waiting for GUS rate limit reset", e);
        }
    }

    @FunctionalInterface
    public interface GusApiRequest<T> {
        T execute();
    }
}
