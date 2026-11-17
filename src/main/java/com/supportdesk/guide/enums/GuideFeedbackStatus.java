package com.supportdesk.guide.enums;

/** Where an agent's step report stands in the admin's queue. New reports are OPEN; the rest is for the admin screen. */
public enum GuideFeedbackStatus
{
    OPEN,
    IN_REVIEW,
    RESOLVED,
    DISMISSED
}
