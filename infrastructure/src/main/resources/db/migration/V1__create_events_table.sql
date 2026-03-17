-- Events table with HASH partitioning by stream_id for scalability
CREATE TABLE events (
    global_offset   BIGSERIAL       NOT NULL,
    stream_id       VARCHAR(255)    NOT NULL,
    stream_version  INT             NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    payload         JSONB           NOT NULL,
    metadata        JSONB           NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    PRIMARY KEY (global_offset, stream_id),
    UNIQUE (stream_id, stream_version)
) PARTITION BY HASH (stream_id);

-- 8 hash partitions - distributes writes across partitions, reduces lock contention
CREATE TABLE events_p0 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 0);
CREATE TABLE events_p1 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 1);
CREATE TABLE events_p2 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 2);
CREATE TABLE events_p3 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 3);
CREATE TABLE events_p4 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 4);
CREATE TABLE events_p5 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 5);
CREATE TABLE events_p6 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 6);
CREATE TABLE events_p7 PARTITION OF events FOR VALUES WITH (MODULUS 8, REMAINDER 7);

-- Index for global reads (ordered by offset)
CREATE INDEX idx_events_global_offset ON events (global_offset);

-- Index for category reads (stream_id prefix match with text_pattern_ops)
CREATE INDEX idx_events_category ON events (stream_id text_pattern_ops);

-- Index for reading a stream's events ordered by version
CREATE INDEX idx_events_stream_version ON events (stream_id, stream_version);
