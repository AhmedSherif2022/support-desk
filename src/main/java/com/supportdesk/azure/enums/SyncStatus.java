package com.supportdesk.azure.enums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Schema(enumAsRef = true, description = "Where the ticket's Azure DevOps work item stands.\n"
        + "- `PENDING`: escalated; the work item is waiting to be sent or is being retried. The agent needs to do nothing.\n"
        + "- `SYNCED`: the work item exists in Azure; its number and state are on the ticket.\n"
        + "- `FAILED`: sending failed 10 times; the failed-integration alert has fired.")
@Getter                        // Lombok: getLabel()
@RequiredArgsConstructor       // Lombok: constructor for the final field
public enum SyncStatus
{
    PENDING("pending · retrying"),   // outbox row waiting or retrying
    SYNCED("synced"),                // work item created (or found) in Azure
    FAILED("failed");                // gave up after the max attempts (E5-S3)

    private final String label;      // the text the ticket shows
}
