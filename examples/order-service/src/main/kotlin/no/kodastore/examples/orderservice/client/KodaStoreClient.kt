package no.kodastore.examples.orderservice.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.time.Instant

@Component
class KodaStoreClient(
    @Value("\${kodastore.url}") baseUrl: String,
    objectMapper: ObjectMapper
) {
    private val rest = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun append(
        streamId: String,
        expectedVersion: Int?,
        events: List<NewEventRequest>
    ): List<RecordedEvent> {
        try {
            return rest.post()
                .uri("/api/streams/{streamId}/events", streamId)
                .body(AppendRequest(expectedVersion, events))
                .retrieve()
                .body(Array<RecordedEvent>::class.java)!!
                .toList()
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                throw ConcurrencyConflictException(streamId, e.responseBodyAsString)
            }
            throw e
        }
    }

    fun readStream(streamId: String, fromVersion: Int = 0): StreamState {
        return rest.get()
            .uri("/api/streams/{streamId}?fromVersion={v}", streamId, fromVersion)
            .retrieve()
            .body(StreamState::class.java)!!
    }
}

data class AppendRequest(
    val expectedVersion: Int?,
    val events: List<NewEventRequest>
)

data class NewEventRequest(
    val eventType: String,
    val payload: Map<String, Any?>,
    val metadata: Map<String, Any?> = emptyMap()
)

data class RecordedEvent(
    val globalOffset: Long,
    val streamId: String,
    val streamVersion: Int,
    val eventType: String,
    val payload: Map<String, Any?>,
    val metadata: Map<String, Any?>,
    val createdAt: Instant
)

data class StreamState(
    val streamId: String,
    val version: Int,
    val events: List<RecordedEvent>
)

class ConcurrencyConflictException(streamId: String, detail: String) :
    RuntimeException("Concurrency conflict on $streamId: $detail")
