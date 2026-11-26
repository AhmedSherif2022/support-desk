package com.supportdesk.azure.client;

import com.supportdesk.azure.exception.AzureDevOpsException;

/**
 * supportdesk.azure.mode=OFF (not configured yet). The outbox worker does not run in this mode, so escalations wait
 * as "pending" and are sent once Azure is configured; this client only guards against an accidental call.
 */
public class OffAzureDevOpsClient implements AzureDevOpsClient
{
    @Override
    public WorkItem createWorkItem(WorkItemRequest request)
    {
        throw off();
    }

    @Override
    public void addComment(long workItemId, String text)
    {
        throw off();
    }

    @Override
    public WorkItem getWorkItem(long workItemId)
    {
        throw off();
    }

    private static AzureDevOpsException off()
    {
        return new AzureDevOpsException("Azure DevOps is not configured (supportdesk.azure.mode=OFF)");
    }
}
