package no.kodastore.domain.model

class OptimisticConcurrencyException(
    val streamId: StreamId,
    val expectedVersion: ExpectedVersion,
    val actualVersion: StreamVersion?
) : RuntimeException(
    "Concurrency conflict on stream ${streamId.value}: expected=$expectedVersion, actual=${actualVersion?.value}"
)

class StreamNotFoundException(val streamId: StreamId) :
    RuntimeException("Stream not found: ${streamId.value}")
