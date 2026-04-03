package no.kodastore.application

import no.kodastore.domain.port.EventStore

class CountCategoryUseCase(private val eventStore: EventStore) {

    fun execute(category: String): Long {
        return eventStore.countCategory(category)
    }
}
