package no.kodastore.infrastructure.web

import no.kodastore.application.*
import no.kodastore.domain.model.RecordedEvent
import no.kodastore.domain.model.StreamState
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api")
class EventController(
    private val appendEventsUseCase: AppendEventsUseCase,
    private val readStreamUseCase: ReadStreamUseCase,
    private val readAllEventsUseCase: ReadAllEventsUseCase,
    private val readCategoryUseCase: ReadCategoryUseCase
) {

    @PostMapping("/streams/{streamId}/events")
    fun appendEvents(
        @PathVariable streamId: String,
        @RequestBody request: AppendEventsRequest
    ): ResponseEntity<List<RecordedEventResponse>> {
        val command = AppendEventsCommand(
            streamId = streamId,
            expectedVersion = request.expectedVersion,
            events = request.events
        )
        val result = appendEventsUseCase.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(result.map { it.toResponse() })
    }

    @GetMapping("/streams/{streamId}")
    fun readStream(
        @PathVariable streamId: String,
        @RequestParam(defaultValue = "0") fromVersion: Int
    ): StreamStateResponse {
        val state = readStreamUseCase.execute(streamId, fromVersion)
        return state.toResponse()
    }

    @GetMapping("/streams")
    fun readAll(
        @RequestParam(defaultValue = "0") fromOffset: Long,
        @RequestParam(defaultValue = "1000") limit: Int
    ): List<RecordedEventResponse> {
        return readAllEventsUseCase.execute(fromOffset, limit).map { it.toResponse() }
    }

    @GetMapping("/categories/{category}")
    fun readCategory(
        @PathVariable category: String,
        @RequestParam(defaultValue = "0") fromOffset: Long,
        @RequestParam(defaultValue = "1000") limit: Int
    ): List<RecordedEventResponse> {
        return readCategoryUseCase.execute(category, fromOffset, limit).map { it.toResponse() }
    }
}

data class AppendEventsRequest(
    val expectedVersion: Int? = null,
    val events: List<NewEventDto>
)

data class RecordedEventResponse(
    val globalOffset: Long,
    val streamId: String,
    val streamVersion: Int,
    val eventType: String,
    val payload: Map<String, Any?>,
    val metadata: Map<String, Any?>,
    val createdAt: Instant
)

data class StreamStateResponse(
    val streamId: String,
    val version: Int,
    val events: List<RecordedEventResponse>
)

private fun RecordedEvent.toResponse() = RecordedEventResponse(
    globalOffset = globalOffset.value,
    streamId = streamId.value,
    streamVersion = streamVersion.value,
    eventType = eventType,
    payload = payload,
    metadata = buildMap {
        metadata.correlationId?.let { put("correlationId", it) }
        metadata.causalityId?.let { put("causalityId", it) }
        metadata.userId?.let { put("userId", it) }
        putAll(metadata.extra)
    },
    createdAt = createdAt
)

private fun StreamState.toResponse() = StreamStateResponse(
    streamId = streamId.value,
    version = version.value,
    events = events.map { it.toResponse() }
)
