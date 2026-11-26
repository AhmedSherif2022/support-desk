package com.supportdesk.azure.exception;

/**
 * Azure DevOps could not be reached or refused the call. The outbox worker catches it and retries later
 * (E5-S3), so it never reaches the agent.
 */
public class AzureDevOpsException extends RuntimeException {
    public AzureDevOpsException(String message) {
        super(message);
    }

    public AzureDevOpsException(String message, Throwable cause) {
        super(message, cause);
    }
}
