package no.kodastore.infrastructure.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.kodastore.domain.model.*
import no.kodastore.domain.port.EventStore
import org.postgresql.util.PSQLException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Repository
class PostgresEventStore(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) : EventStore {

    @Transactional
    override fun append(
        streamId: StreamId,
        expectedVersion: ExpectedVersion,
        events: List<NewEvent>
    ): List<RecordedEvent> {
        require(events.isNotEmpty()) { "Must append at least one event" }

        val currentVersion = getCurrentVersion(streamId)
        validateExpectedVersion(streamId, expectedVersion, currentVersion)

        val startVersion = currentVersion?.value ?: 0

        return events.mapIndexed { index, event ->
            val version = startVersion + index + 1
            val payloadJson = objectMapper.writeValueAsString(event.payload)
            val metadataJson = objectMapper.writeValueAsString(
                mapOf(
                    "correlationId" to event.metadata.correlationId,
                    "causalityId" to event.metadata.causalityId,
                    "userId" to event.metadata.userId
                ) + event.metadata.extra
            )

            try {
                jdbc.queryForObject(
                    """
                    INSERT INTO events (stream_id, stream_version, event_type, payload, metadata)
                    VALUES (?, ?, ?, ?::jsonb, ?::jsonb)
                    RETURNING global_offset, created_at
                    """.trimIndent(),
                    { rs, _ ->
                        RecordedEvent(
                            globalOffset = GlobalOffset(rs.getLong("global_offset")),
                            streamId = streamId,
                            streamVersion = StreamVersion(version),
                            eventType = event.eventType,
                            payload = event.payload,
                            metadata = event.metadata,
                            createdAt = rs.getTimestamp("created_at").toInstant()
                        )
                    },
                    streamId.value,
                    version,
                    event.eventType,
                    payloadJson,
                    metadataJson
                )!!
            } catch (e: DuplicateKeyException) {
                throw OptimisticConcurrencyException(
                    streamId,
                    expectedVersion,
                    currentVersion
                )
            }
        }
    }

    override fun readStream(streamId: StreamId, fromVersion: StreamVersion): StreamState {
        val events = jdbc.query(
            """
            SELECT global_offset, stream_id, stream_version, event_type, payload, metadata, created_at
            FROM events
            WHERE stream_id = ? AND stream_version >= ?
            ORDER BY stream_version
            """.trimIndent(),
            { rs, _ -> mapEvent(rs) },
            streamId.value,
            fromVersion.value
        )

        val maxVersion = events.maxOfOrNull { it.streamVersion.value } ?: 0

        return StreamState(
            streamId = streamId,
            version = StreamVersion(maxVersion),
            events = events
        )
    }

    override fun readAll(fromOffset: GlobalOffset, limit: Int): List<RecordedEvent> {
        return jdbc.query(
            """
            SELECT global_offset, stream_id, stream_version, event_type, payload, metadata, created_at
            FROM events
            WHERE global_offset > ?
            ORDER BY global_offset
            LIMIT ?
            """.trimIndent(),
            { rs, _ -> mapEvent(rs) },
            fromOffset.value,
            limit
        )
    }

    override fun readCategory(
        category: String,
        fromOffset: GlobalOffset,
        limit: Int
    ): List<RecordedEvent> {
        return jdbc.query(
            """
            SELECT global_offset, stream_id, stream_version, event_type, payload, metadata, created_at
            FROM events
            WHERE stream_id LIKE ? AND global_offset > ?
            ORDER BY global_offset
            LIMIT ?
            """.trimIndent(),
            { rs, _ -> mapEvent(rs) },
            "$category-%",
            fromOffset.value,
            limit
        )
    }

    private fun getCurrentVersion(streamId: StreamId): StreamVersion? {
        val version = jdbc.queryForObject(
            "SELECT MAX(stream_version) FROM events WHERE stream_id = ?",
            Int::class.java,
            streamId.value
        )
        return version?.let { StreamVersion(it) }
    }

    private fun validateExpectedVersion(
        streamId: StreamId,
        expected: ExpectedVersion,
        actual: StreamVersion?
    ) {
        when (expected) {
            ExpectedVersion.Any -> {}
            ExpectedVersion.NoStream -> {
                if (actual != null) {
                    throw OptimisticConcurrencyException(streamId, expected, actual)
                }
            }
            is ExpectedVersion.Exact -> {
                if (actual?.value != expected.version.value) {
                    throw OptimisticConcurrencyException(streamId, expected, actual)
                }
            }
        }
    }

    private fun mapEvent(rs: ResultSet): RecordedEvent {
        val metadataMap: Map<String, Any?> = objectMapper.readValue(rs.getString("metadata"))
        return RecordedEvent(
            globalOffset = GlobalOffset(rs.getLong("global_offset")),
            streamId = StreamId(rs.getString("stream_id")),
            streamVersion = StreamVersion(rs.getInt("stream_version")),
            eventType = rs.getString("event_type"),
            payload = objectMapper.readValue(rs.getString("payload")),
            metadata = EventMetadata(
                correlationId = metadataMap["correlationId"] as? String,
                causalityId = metadataMap["causalityId"] as? String,
                userId = metadataMap["userId"] as? String,
                extra = metadataMap.filterKeys { it !in setOf("correlationId", "causalityId", "userId") }
            ),
            createdAt = rs.getTimestamp("created_at").toInstant()
        )
    }
}
