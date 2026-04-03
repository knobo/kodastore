package no.kodastore.application

import no.kodastore.domain.model.StreamId
import no.kodastore.domain.port.EventStore

class StreamExistsUseCase(private val eventStore: EventStore) {

    fun execute(streamId: String): Boolean {
        return eventStore.streamExists(StreamId(streamId))
    }
}
