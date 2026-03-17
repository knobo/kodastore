package no.kodastore.domain.model

@JvmInline
value class GlobalOffset(val value: Long) {
    init {
        require(value >= 0) { "GlobalOffset must be >= 0, got: $value" }
    }

    companion object {
        val START = GlobalOffset(0)
    }
}
