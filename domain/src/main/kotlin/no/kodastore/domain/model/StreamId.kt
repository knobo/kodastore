package no.kodastore.domain.model

@JvmInline
value class StreamId(val value: String) {
    init {
        require(value.contains('-')) { "StreamId must follow Category-ID format, got: $value" }
        require(value.substringBefore('-').isNotBlank()) { "Category must not be blank" }
        require(value.substringAfter('-').isNotBlank()) { "Entity ID must not be blank" }
    }

    val category: String get() = value.substringBefore('-')
    val entityId: String get() = value.substringAfter('-')
}
