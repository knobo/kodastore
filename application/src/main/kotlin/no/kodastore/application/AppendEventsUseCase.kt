package no.kodastore.application

import no.kodastore.domain.model.*
import no.kodastore.domain.port.EventStore

class AppendEventsUseCase(private val eventStore: EventStore) {

    fun execute(command: AppendEventsCommand): List<RecordedEvent> {
        return eventStore.append(
            streamId = StreamId(command.streamId),
            expectedVersion = command.toExpectedVersion(),
            events = command.events.map { it.toDomain() }
        )
    }
}

data class AppendEventsCommand(
    val streamId: String,
    val expectedVersion: Int?,
    val events: List<NewEventDto>
) {
    fun toExpectedVersion(): ExpectedVersion = when (expectedVersion) {
        null -> ExpectedVersion.Any
        -1 -> ExpectedVersion.NoStream
        else -> ExpectedVersion.Exact(StreamVersion(expectedVersion))
    }
}

data class NewEventDto(
    val eventType: String,
    val payload: Map<String, Any?>,
    val metadata: EventMetadataDto = EventMetadataDto()
) {
    fun toDomain() = NewEvent(
        eventType = eventType,
        payload = payload,
        metadata = EventMetadata(
            correlationId = metadata.correlationId,
            causalityId = metadata.causalityId,
            userId = metadata.userId,
            extra = metadata.extra
        )
    )
}

data class EventMetadataDto(
    val correlationId: String? = null,
    val causalityId: String? = null,
    val userId: String? = null,
    val extra: Map<String, Any?> = emptyMap()
)
