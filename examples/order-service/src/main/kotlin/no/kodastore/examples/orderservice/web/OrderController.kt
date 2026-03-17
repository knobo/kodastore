package no.kodastore.examples.orderservice.web

import no.kodastore.examples.orderservice.application.OrderNotFoundException
import no.kodastore.examples.orderservice.application.OrderService
import no.kodastore.examples.orderservice.client.ConcurrencyConflictException
import no.kodastore.examples.orderservice.domain.OrderState
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    fun createOrder(@RequestBody req: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(req.customerId)
        return ResponseEntity.status(HttpStatus.CREATED).body(order.toResponse())
    }

    @PostMapping("/{orderId}/items")
    fun addItem(
        @PathVariable orderId: String,
        @RequestBody req: AddItemRequest
    ): OrderResponse {
        return orderService.addItem(orderId, req.productName, req.quantity, req.unitPrice).toResponse()
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    fun removeItem(
        @PathVariable orderId: String,
        @PathVariable itemId: String
    ): OrderResponse {
        return orderService.removeItem(orderId, itemId).toResponse()
    }

    @PostMapping("/{orderId}/confirm")
    fun confirmOrder(@PathVariable orderId: String): OrderResponse {
        return orderService.confirmOrder(orderId).toResponse()
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: String,
        @RequestBody req: CancelOrderRequest
    ): OrderResponse {
        return orderService.cancelOrder(orderId, req.reason).toResponse()
    }

    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: String): OrderResponse {
        return orderService.getOrder(orderId).toResponse()
    }

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleNotFound(ex: OrderNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("NOT_FOUND", ex.message ?: "Order not found"))
    }

    @ExceptionHandler(ConcurrencyConflictException::class)
    fun handleConflict(ex: ConcurrencyConflictException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("CONFLICT", ex.message ?: "Concurrency conflict"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("BAD_REQUEST", ex.message ?: "Invalid request"))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleInvalidState(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse("INVALID_STATE", ex.message ?: "Invalid state transition"))
    }
}

// ===== DTOs =====

data class CreateOrderRequest(val customerId: String)
data class AddItemRequest(val productName: String, val quantity: Int, val unitPrice: Double)
data class CancelOrderRequest(val reason: String)
data class ErrorResponse(val code: String, val message: String)

data class OrderItemResponse(
    val itemId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

data class OrderResponse(
    val orderId: String,
    val customerId: String,
    val status: String,
    val items: List<OrderItemResponse>,
    val totalAmount: Double,
    val version: Int
)

private fun OrderState.toResponse() = OrderResponse(
    orderId = orderId,
    customerId = customerId,
    status = status.name,
    items = items.map {
        OrderItemResponse(it.itemId, it.productName, it.quantity, it.unitPrice, it.totalPrice)
    },
    totalAmount = totalAmount,
    version = version
)
