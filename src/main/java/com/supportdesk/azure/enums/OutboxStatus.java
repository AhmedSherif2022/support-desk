package com.supportdesk.azure.enums;

/** Where one outbox message stands. */
public enum OutboxStatus
{
    PENDING,   // waiting for its first try, or for its next retry (next_attempt_at)
    SENT,      // Azure accepted it
    FAILED     // gave up after the maximum attempts; the failed-integration alert fired
}
