package com.supportdesk.azure.client;

import com.supportdesk.azure.exception.AzureDevOpsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory Azure DevOps for local runs and tests (supportdesk.azure.mode=FAKE). Same idempotency rule as the real
 * client: one work item per key. {@link #setAvailable(boolean)} simulates an Azure outage (E5-S3).
 */
public class FakeAzureDevOpsClient implements AzureDevOpsClient
{
    private final AtomicLong nextId = new AtomicLong(70_001);
    private final Map<String, WorkItem> byKey = new ConcurrentHashMap<>();
    private final Map<Long, WorkItemRequest> requests = new ConcurrentHashMap<>();
    private final List<String> comments = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean available = true;

    @Override
    public WorkItem createWorkItem(WorkItemRequest request)
    {
        checkAvailable();
        return byKey.computeIfAbsent(request.idempotencyKey(), key -> {
            WorkItem item = new WorkItem(nextId.getAndIncrement(), "New", 1);
            requests.put(item.id(), request);
            return item;
        });
    }

    @Override
    public void addComment(long workItemId, String text)
    {
        checkAvailable();
        comments.add(workItemId + ": " + text);
    }

    @Override
    public WorkItem getWorkItem(long workItemId)
    {
        checkAvailable();
        return byKey.values().stream().filter(w -> w.id() == workItemId).findFirst()
                .orElseThrow(() -> new AzureDevOpsException("No work item " + workItemId));
    }


    /** false = every call fails as if Azure were down. */
    public void setAvailable(boolean available)
    {
        this.available = available;
    }

    /** What was sent for a work item (tests check it carries no personal data). */
    public WorkItemRequest requestFor(long workItemId)
    {
        return requests.get(workItemId);
    }

    public long createdCountFor(String idempotencyKey)
    {
        return byKey.containsKey(idempotencyKey) ? 1 : 0;
    }

    public List<String> comments()
    {
        return List.copyOf(comments);
    }

    private void checkAvailable()
    {
        if (!available) {
            throw new AzureDevOpsException("Azure DevOps is unavailable (fake outage)");
        }
    }
}
