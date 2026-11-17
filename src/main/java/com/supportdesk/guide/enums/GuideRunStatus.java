package com.supportdesk.guide.enums;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Where a guide run is.\n"
        + "- IN_PROGRESS: answering questions\n"
        + "- OUTCOME_REACHED: the last answer led to an outcome; finish the run with `POST /api/guide-runs/{id}/complete`\n"
        + "- COMPLETED: finished, with its outcome applied to the ticket\n"
        + "- ABANDONED: ended without an outcome applied, because a new run started on the ticket")
public enum GuideRunStatus
{
    IN_PROGRESS,
    /** The last answer led to an outcome; the run is finished once that outcome is applied (E4-S3). */
    OUTCOME_REACHED,
    COMPLETED,
    ABANDONED
}
