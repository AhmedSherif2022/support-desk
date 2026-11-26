package com.supportdesk.azure.client;

import java.util.List;

/**
 * The only way the portal talks to Azure DevOps. Implementations: {@link RestAzureDevOpsClient} (real API),
 * {@link FakeAzureDevOpsClient} (local runs and tests), {@link OffAzureDevOpsClient} (not configured).
 * Every method throws {@link com.supportdesk.azure.exception.AzureDevOpsException} when Azure cannot be reached;
 * only the outbox worker calls it, so a failure never blocks the agent (E5-S3).
 */
public interface AzureDevOpsClient
{
    /**
     * Creates the work item, or returns the one already created for {@code request.idempotencyKey()}, so a retry
     * after a lost response never makes a second work item (E5-S3).
     */
    WorkItem createWorkItem(WorkItemRequest request);

    /** Adds a comment (E5-S6 "Ask SRE for update", E5-S7 "Still not working"). */
    void addComment(long workItemId, String text);

    /** Reads a work item's current state (Member 2: reconciliation job). */
    WorkItem getWorkItem(long workItemId);


    /**
     * What to create. Never contains CIF, national ID, mobile or the customer's name (E5-S1).
     *
     * @param idempotencyKey e.g. TICKET-2001; stored on the work item as a tag
     * @param priority       Azure priority 1 (highest) to 4
     */
    record WorkItemRequest(String idempotencyKey, String title, String descriptionHtml, int priority, List<String> tags)
    {
        /** The tag that marks the work item as the one for this key. */
        public String keyTag()
        {
            return "PortalKey-" + idempotencyKey;
        }
    }

    /** A work item as Azure returns it. */
    record WorkItem(long id, String state, int revision)
    {
    }
}
