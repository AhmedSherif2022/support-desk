package com.supportdesk.guide.enums;

public enum GuideRunStatus
{
    IN_PROGRESS,
    /** The last answer led to an outcome; the run is finished once that outcome is applied (E4-S3). */
    OUTCOME_REACHED,
    COMPLETED
}
