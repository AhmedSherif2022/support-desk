package com.supportdesk.azure.enums;

/** What an outbox message asks Azure DevOps to do. */
public enum OutboxOperation
{
    CREATE_WORK_ITEM,   // E5-S1 (ticket) and E6 incidents (Member 2): one work item per idempotency key
    ADD_COMMENT         // E5-S6 "Ask SRE for update", E5-S7 "Still not working" (Member 3)
}
