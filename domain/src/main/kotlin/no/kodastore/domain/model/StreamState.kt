package no.kodastore.domain.model

data class StreamState(
    val streamId: StreamId,
    val version: StreamVersion,
    val events: List<RecordedEvent>
)
