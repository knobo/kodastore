package no.kodastore.domain.model

import java.time.Instant

data class RecordedEvent(
    val globalOffset: GlobalOffset,
    val streamId: StreamId,
    val streamVersion: StreamVersion,
    val eventType: String,
    val payload: Map<String, Any?>,
    val metadata: EventMetadata,
    val createdAt: Instant
)

data class NewEvent(
    val eventType: String,
    val payload: Map<String, Any?>,
    val metadata: EventMetadata = EventMetadata()
)
