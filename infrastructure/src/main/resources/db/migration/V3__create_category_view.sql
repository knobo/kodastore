-- Virtual stream view using category extraction from stream_id
CREATE OR REPLACE VIEW category_streams AS
SELECT
    split_part(stream_id, '-', 1) AS category,
    stream_id,
    global_offset,
    stream_version,
    event_type,
    payload,
    metadata,
    created_at
FROM events;
