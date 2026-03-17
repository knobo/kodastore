package no.kodastore.application

import no.kodastore.domain.model.*
import no.kodastore.domain.port.EventStore

class ReadAllEventsUseCase(private val eventStore: EventStore) {

    fun execute(fromOffset: Long = 0, limit: Int = 1000): List<RecordedEvent> {
        return eventStore.readAll(GlobalOffset(fromOffset), limit)
    }
}
