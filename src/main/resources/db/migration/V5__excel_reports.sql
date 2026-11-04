ALTER TABLE report_jobs DROP CONSTRAINT report_jobs_format_check;
ALTER TABLE report_jobs ALTER COLUMN format TYPE VARCHAR(4);
ALTER TABLE report_jobs ADD CONSTRAINT report_jobs_format_check CHECK (format IN ('pdf','csv','xlsx'));

UPDATE report_artifacts a SET content_type = CASE j.format
    WHEN 'pdf' THEN 'application/pdf'
    WHEN 'xlsx' THEN 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ELSE 'text/csv'
END FROM report_jobs j WHERE j.id = a.report_id;
