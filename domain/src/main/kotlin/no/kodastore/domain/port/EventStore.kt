package no.kodastore.domain.port

import no.kodastore.domain.model.*

interface EventStore {

    fun append(
        streamId: StreamId,
        expectedVersion: ExpectedVersion,
        events: List<NewEvent>
    ): List<RecordedEvent>

    fun readStream(
        streamId: StreamId,
        fromVersion: StreamVersion = StreamVersion(0)
    ): StreamState

    fun readAll(
        fromOffset: GlobalOffset = GlobalOffset.START,
        limit: Int = 1000
    ): List<RecordedEvent>

    fun readCategory(
        category: String,
        fromOffset: GlobalOffset = GlobalOffset.START,
        limit: Int = 1000
    ): List<RecordedEvent>

    fun countCategory(category: String): Long

    fun streamExists(streamId: StreamId): Boolean
}
