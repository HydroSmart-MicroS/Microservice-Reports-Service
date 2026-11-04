CREATE TABLE report_jobs (
    id UUID PRIMARY KEY,
    requested_by_user_id TEXT NOT NULL,
    unit_id UUID,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    format VARCHAR(3) NOT NULL CHECK (format IN ('pdf','csv')),
    status VARCHAR(12) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','processing','completed','failed')),
    storage_key TEXT,
    error_code TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CHECK (period_end > period_start)
);
CREATE INDEX report_jobs_user_idx ON report_jobs (requested_by_user_id, requested_at DESC);


CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    event_type TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0)
);
CREATE INDEX outbox_pending_idx ON outbox_events (occurred_at) WHERE published_at IS NULL;
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
