-- The agreed ticket diagram adds In Review: an SRE fix waits here for a Reviewer (Send to review).
ALTER TABLE tickets DROP CONSTRAINT ck_tickets_status;
ALTER TABLE tickets ADD CONSTRAINT ck_tickets_status CHECK (status IN ('NEW', 'TROUBLESHOOTING', 'WAITING_ON_CUSTOMER',
    'ESCALATED', 'WITH_SRE', 'SRE_FIXED', 'IN_REVIEW', 'RESOLVED', 'REOPENED', 'CLOSED'));
