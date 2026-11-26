package com.supportdesk.azure.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** E5-S3: exponential backoff, capped. */
class OutboxBackoffTest
{
    private static final Duration FIRST = Duration.ofSeconds(30);
    private static final Duration MAX = Duration.ofHours(1);

    @Test
    void delayDoublesAfterEachFailureUpToTheCap()
    {
        assertThat(OutboxMessageProcessor.backoff(1, FIRST, MAX)).isEqualTo(Duration.ofSeconds(30));
        assertThat(OutboxMessageProcessor.backoff(2, FIRST, MAX)).isEqualTo(Duration.ofMinutes(1));
        assertThat(OutboxMessageProcessor.backoff(3, FIRST, MAX)).isEqualTo(Duration.ofMinutes(2));
        assertThat(OutboxMessageProcessor.backoff(7, FIRST, MAX)).isEqualTo(Duration.ofMinutes(32));
        assertThat(OutboxMessageProcessor.backoff(8, FIRST, MAX)).isEqualTo(MAX);
        assertThat(OutboxMessageProcessor.backoff(40, FIRST, MAX)).isEqualTo(MAX);   // no overflow
    }
}
