CREATE TABLE snapshots (
    stream_id       VARCHAR(255)    PRIMARY KEY,
    stream_version  INT             NOT NULL,
    state           JSONB           NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
