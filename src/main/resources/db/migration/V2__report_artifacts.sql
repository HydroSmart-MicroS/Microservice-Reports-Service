CREATE TABLE report_artifacts(report_id UUID PRIMARY KEY REFERENCES report_jobs(id),content BYTEA NOT NULL,content_type TEXT NOT NULL DEFAULT 'text/csv');

