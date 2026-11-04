ALTER TABLE report_jobs ADD COLUMN request_event_id UUID;
ALTER TABLE report_jobs ADD COLUMN last_error VARCHAR(300);
UPDATE report_jobs j
SET request_event_id=(
    SELECT e.event_id FROM outbox_events e
    WHERE e.aggregate_id=j.id::text AND e.event_type='reports.report.requested.v1'
    ORDER BY e.occurred_at DESC LIMIT 1
);
