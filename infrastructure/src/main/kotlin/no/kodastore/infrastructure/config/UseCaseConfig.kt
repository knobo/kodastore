package no.kodastore.infrastructure.config

import no.kodastore.application.*
import no.kodastore.domain.port.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UseCaseConfig {

    @Bean
    fun appendEventsUseCase(eventStore: EventStore) = AppendEventsUseCase(eventStore)

    @Bean
    fun readStreamUseCase(eventStore: EventStore) = ReadStreamUseCase(eventStore)

    @Bean
    fun readAllEventsUseCase(eventStore: EventStore) = ReadAllEventsUseCase(eventStore)

    @Bean
    fun readCategoryUseCase(eventStore: EventStore) = ReadCategoryUseCase(eventStore)
}
