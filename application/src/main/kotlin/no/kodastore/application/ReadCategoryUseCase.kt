package no.kodastore.application

import no.kodastore.domain.model.*
import no.kodastore.domain.port.EventStore

class ReadCategoryUseCase(private val eventStore: EventStore) {

    fun execute(category: String, fromOffset: Long = 0, limit: Int = 1000): List<RecordedEvent> {
        return eventStore.readCategory(category, GlobalOffset(fromOffset), limit)
    }
}
