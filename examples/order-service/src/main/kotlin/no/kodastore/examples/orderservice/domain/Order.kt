package no.kodastore.examples.orderservice.domain

import java.time.Instant
import java.util.UUID

// ===== Events =====

sealed interface OrderEvent {
    val orderId: String
}

data class OrderCreated(
    override val orderId: String,
    val customerId: String,
    val createdAt: Instant = Instant.now()
) : OrderEvent

data class ItemAdded(
    override val orderId: String,
    val itemId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
) : OrderEvent

data class ItemRemoved(
    override val orderId: String,
    val itemId: String
) : OrderEvent

data class OrderConfirmed(
    override val orderId: String,
    val confirmedAt: Instant = Instant.now()
) : OrderEvent

data class OrderCancelled(
    override val orderId: String,
    val reason: String,
    val cancelledAt: Instant = Instant.now()
) : OrderEvent

// ===== State =====

data class OrderItem(
    val itemId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val totalPrice: Double get() = quantity * unitPrice
}

enum class OrderStatus { DRAFT, CONFIRMED, CANCELLED }

data class OrderState(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.DRAFT,
    val version: Int = 0
) {
    val totalAmount: Double get() = items.sumOf { it.totalPrice }

    companion object {
        val EMPTY = OrderState("", "", emptyList(), OrderStatus.DRAFT, 0)

        fun evolve(state: OrderState, event: OrderEvent, version: Int): OrderState = when (event) {
            is OrderCreated -> state.copy(
                orderId = event.orderId,
                customerId = event.customerId,
                version = version
            )
            is ItemAdded -> state.copy(
                items = state.items + OrderItem(event.itemId, event.productName, event.quantity, event.unitPrice),
                version = version
            )
            is ItemRemoved -> state.copy(
                items = state.items.filter { it.itemId != event.itemId },
                version = version
            )
            is OrderConfirmed -> state.copy(
                status = OrderStatus.CONFIRMED,
                version = version
            )
            is OrderCancelled -> state.copy(
                status = OrderStatus.CANCELLED,
                version = version
            )
        }

        fun rebuild(events: List<Pair<OrderEvent, Int>>): OrderState {
            return events.fold(EMPTY) { state, (event, version) ->
                evolve(state, event, version)
            }
        }
    }
}
