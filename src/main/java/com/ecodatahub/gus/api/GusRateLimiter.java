package com.ecodatahub.gus.api;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class GusRateLimiter {

    private final Bucket bucket;
    private final Duration minimumDelay;
    private Instant lastRequestAt = Instant.MIN;

    public GusRateLimiter(
            @Value("${gus.request-delay:10s}")
            Duration minimumDelay
    ) {
        this.minimumDelay = minimumDelay;

        Bandwidth secondLimit = Bandwidth.classic(
                5,
                Refill.intervally(
                        5,
                        Duration.ofSeconds(1)
                )
        );

        Bandwidth fifteenMinuteLimit = Bandwidth.classic(
                100,
                Refill.intervally(
                        100,
                        Duration.ofMinutes(15)
                )
        );

        Bandwidth twelveHourLimit = Bandwidth.classic(
                1000,
                Refill.intervally(
                        1000,
                        Duration.ofHours(12)
                )
        );

        Bandwidth sevenDayLimit = Bandwidth.classic(
                10000,
                Refill.intervally(
                        10000,
                        Duration.ofDays(7)
                )
        );

        this.bucket = Bucket.builder()
                .addLimit(secondLimit)
                .addLimit(fifteenMinuteLimit)
                .addLimit(twelveHourLimit)
                .addLimit(sevenDayLimit)
                .build();
    }

    public void consume() {
        try {
            waitForMinimumDelay();
            bucket.asBlocking().consume(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Thread interrupted while waiting for GUS rate limit",
                    e
            );
        }
    }

    private synchronized void waitForMinimumDelay() throws InterruptedException {
        Instant now = Instant.now();

        if (!Instant.MIN.equals(lastRequestAt)) {
            Duration timeSinceLastRequest = Duration.between(lastRequestAt, now);

            if (timeSinceLastRequest.compareTo(minimumDelay) < 0) {
                Thread.sleep(minimumDelay.minus(timeSinceLastRequest).toMillis());
            }
        }

        lastRequestAt = Instant.now();
    }
}
