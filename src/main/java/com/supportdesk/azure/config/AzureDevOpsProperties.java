package com.supportdesk.azure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * supportdesk.azure.* settings. Secrets (the personal access token, the customer token secret) come from
 * environment variables and are never stored in git.
 *
 * @param mode                 OFF: nothing is sent (escalations wait as "pending"); FAKE: in-memory Azure for local
 *                             runs and tests; REST: the real Azure DevOps REST API.
 * @param organization         Azure DevOps organization (dev.azure.com/{organization})
 * @param project              Azure DevOps project
 * @param workItemType         work item type to create, e.g. Bug or Issue
 * @param personalAccessToken  PAT with "Work Items (read and write)"
 * @param portalBaseUrl        base URL of the portal, for the ticket link in the work item
 * @param customerTokenSecret  key for the masked customer token (HMAC); never sent anywhere
 */
@Validated
@ConfigurationProperties(prefix = "supportdesk.azure")
public record AzureDevOpsProperties(
        @DefaultValue("OFF") Mode mode,
        String organization,
        String project,
        @DefaultValue("Bug") String workItemType,
        String personalAccessToken,
        @DefaultValue("http://localhost:4200") String portalBaseUrl,
        @NotNull String customerTokenSecret,
        @Valid @DefaultValue Outbox outbox)
{
    public enum Mode
    {
        OFF,
        FAKE,
        REST
    }

    /**
     * @param pollInterval     how often the worker looks for due messages
     * @param batchSize        most messages sent per poll
     * @param maxAttempts      after this many failures a message is FAILED and the alert fires (E5-S3: 10)
     * @param firstRetryDelay  wait after the first failure; doubles after each failure
     * @param maxRetryDelay    the doubling stops here
     */
    public record Outbox(
            @DefaultValue("15s") Duration pollInterval,
            @DefaultValue("20") @Min(1) int batchSize,
            @DefaultValue("10") @Min(1) int maxAttempts,
            @DefaultValue("30s") Duration firstRetryDelay,
            @DefaultValue("1h") Duration maxRetryDelay)
    {
    }

    public boolean sending()
    {
        return mode != Mode.OFF;
    }
}
