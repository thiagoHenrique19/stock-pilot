CREATE TABLE idempotency_key (
    scope             VARCHAR(100)                NOT NULL,
    key               VARCHAR(255)                NOT NULL,
    request_hash      CHAR(64)                    NOT NULL,
    status            VARCHAR(20)                 NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    response_snapshot TEXT,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at      TIMESTAMP(6) WITH TIME ZONE,
    PRIMARY KEY (scope, key)
);
