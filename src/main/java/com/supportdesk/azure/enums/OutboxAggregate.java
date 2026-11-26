package com.supportdesk.azure.enums;

/** What an outbox message belongs to; with the aggregate id it says which work item to create or comment on. */
public enum OutboxAggregate
{
    TICKET,
    INCIDENT
}
