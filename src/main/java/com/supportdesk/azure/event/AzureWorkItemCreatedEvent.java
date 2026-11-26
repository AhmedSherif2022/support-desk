package com.supportdesk.azure.event;

import com.supportdesk.azure.enums.OutboxAggregate;

/**
 * Published when the outbox worker has created a work item. Tickets are updated by Member 1's code; Member 2 listens
 * for INCIDENT work items (E5-S2) and updates the incident and its linked tickets.
 */
public record AzureWorkItemCreatedEvent(OutboxAggregate aggregateType, Long aggregateId, String idempotencyKey,
                                        long workItemId, String state, int revision)
{
}
