package no.kodastore.domain.model

sealed interface ExpectedVersion {
    data object NoStream : ExpectedVersion
    data class Exact(val version: StreamVersion) : ExpectedVersion
    data object Any : ExpectedVersion
}
