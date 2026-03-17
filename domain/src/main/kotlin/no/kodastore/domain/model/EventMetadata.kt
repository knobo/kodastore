package no.kodastore.domain.model

data class EventMetadata(
    val correlationId: String? = null,
    val causalityId: String? = null,
    val userId: String? = null,
    val extra: Map<String, Any?> = emptyMap()
)
