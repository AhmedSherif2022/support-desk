-- The ticket Work tab lists every guide run on a ticket, newest first.
CREATE INDEX idx_troubleshooting_sessions_ticket ON troubleshooting_sessions (ticket_id, started_at DESC);
