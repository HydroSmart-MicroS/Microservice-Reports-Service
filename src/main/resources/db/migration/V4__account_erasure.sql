CREATE TABLE privacy_receipts(user_hash TEXT PRIMARY KEY,completed BOOLEAN NOT NULL DEFAULT false,expires_at TIMESTAMPTZ NOT NULL);
