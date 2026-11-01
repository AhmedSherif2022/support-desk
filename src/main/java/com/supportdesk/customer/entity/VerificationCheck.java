package com.supportdesk.customer.entity;

/** Identity checks an agent can tick during a call. At least two must pass (E2). */
public enum VerificationCheck
{
    DATE_OF_BIRTH,
    NATIONAL_ID_LAST4,
    MOBILE_ON_FILE,
    RECENT_TRANSACTION
}
