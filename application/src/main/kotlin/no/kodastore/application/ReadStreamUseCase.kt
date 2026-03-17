package no.kodastore.application

import no.kodastore.domain.model.*
import no.kodastore.domain.port.EventStore

class ReadStreamUseCase(private val eventStore: EventStore) {

    fun execute(streamId: String, fromVersion: Int = 0): StreamState {
        return eventStore.readStream(
            StreamId(streamId),
            StreamVersion(fromVersion)
        )
    }
}
