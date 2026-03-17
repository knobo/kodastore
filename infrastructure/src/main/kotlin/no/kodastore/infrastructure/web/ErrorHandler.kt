package no.kodastore.infrastructure.web

import no.kodastore.domain.model.OptimisticConcurrencyException
import no.kodastore.domain.model.StreamNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ErrorHandler {

    @ExceptionHandler(OptimisticConcurrencyException::class)
    fun handleConcurrencyConflict(ex: OptimisticConcurrencyException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("CONCURRENCY_CONFLICT", ex.message ?: "Concurrency conflict"))
    }

    @ExceptionHandler(StreamNotFoundException::class)
    fun handleStreamNotFound(ex: StreamNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("STREAM_NOT_FOUND", ex.message ?: "Stream not found"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("BAD_REQUEST", ex.message ?: "Invalid request"))
    }
}

data class ErrorResponse(
    val code: String,
    val message: String
)
