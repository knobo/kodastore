package no.kodastore.domain.model

@JvmInline
value class StreamVersion(val value: Int) {
    init {
        require(value >= 0) { "Version must be >= 0, got: $value" }
    }

    fun next(): StreamVersion = StreamVersion(value + 1)
}
