package com.supportdesk.azure.service;

import com.supportdesk.azure.config.AzureDevOpsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * E5-S3: sends due outbox messages every poll interval. With supportdesk.azure.mode=OFF nothing is sent and
 * escalations wait as "pending"; they go out once Azure DevOps is configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker
{
    private final OutboxMessageProcessor processor;
    private final AzureDevOpsProperties props;

    @Scheduled(fixedDelayString = "${supportdesk.azure.outbox.poll-interval:15s}",
            initialDelayString = "${supportdesk.azure.outbox.poll-interval:15s}")
    public void scheduledRun()
    {
        if (props.sending()) {
            runOnce();
        }
    }

    /** Sends up to batch-size due messages; returns how many were handled (sent, retried or failed). */
    public int runOnce()
    {
        int handled = 0;
        while (handled < props.outbox().batchSize()) {
            try {
                if (!processor.processNext()) {
                    break;
                }
            } catch (RuntimeException e) {
                log.error("Azure outbox poll failed", e);   // e.g. database down; the next poll tries again
                break;
            }
            handled++;
        }
        return handled;
    }
}
