-- Service health (agent home): the DEGRADED window and threshold, separate from incident grouping.
-- Distinct customers in the window, not error rows. Defaults match the grouping defaults on purpose
-- (5 customers, 15 minutes) but a Guide Admin changes them independently.
INSERT INTO app_settings (setting_key, setting_value, updated_at) VALUES
    ('service-health.degraded.window-minutes', '15', now()),
    ('service-health.degraded.min-customers',   '5',  now())
ON CONFLICT (setting_key) DO NOTHING;

-- The home panel counts errors by platform, service and time. The existing index starts with error_code.
CREATE INDEX ix_detected_error_event_service_time
    ON detected_error_event (platform, service, occurred_at);
