package no.kodastore.examples.orderservice.domain

import no.kodastore.examples.orderservice.client.NewEventRequest
import no.kodastore.examples.orderservice.client.RecordedEvent
import java.time.Instant

object EventMapper {

    fun toRequest(event: OrderEvent): NewEventRequest {
        val (type, payload) = when (event) {
            is OrderCreated -> "OrderCreated" to mapOf(
                "orderId" to event.orderId,
                "customerId" to event.customerId,
                "createdAt" to event.createdAt.toString()
            )
            is ItemAdded -> "ItemAdded" to mapOf(
                "orderId" to event.orderId,
                "itemId" to event.itemId,
                "productName" to event.productName,
                "quantity" to event.quantity,
                "unitPrice" to event.unitPrice
            )
            is ItemRemoved -> "ItemRemoved" to mapOf(
                "orderId" to event.orderId,
                "itemId" to event.itemId
            )
            is OrderConfirmed -> "OrderConfirmed" to mapOf(
                "orderId" to event.orderId,
                "confirmedAt" to event.confirmedAt.toString()
            )
            is OrderCancelled -> "OrderCancelled" to mapOf(
                "orderId" to event.orderId,
                "reason" to event.reason,
                "cancelledAt" to event.cancelledAt.toString()
            )
        }
        return NewEventRequest(type, payload)
    }

    fun fromRecorded(recorded: RecordedEvent): Pair<OrderEvent, Int> {
        val p = recorded.payload
        val event: OrderEvent = when (recorded.eventType) {
            "OrderCreated" -> OrderCreated(
                orderId = p["orderId"] as String,
                customerId = p["customerId"] as String,
                createdAt = Instant.parse(p["createdAt"] as String)
            )
            "ItemAdded" -> ItemAdded(
                orderId = p["orderId"] as String,
                itemId = p["itemId"] as String,
                productName = p["productName"] as String,
                quantity = (p["quantity"] as Number).toInt(),
                unitPrice = (p["unitPrice"] as Number).toDouble()
            )
            "ItemRemoved" -> ItemRemoved(
                orderId = p["orderId"] as String,
                itemId = p["itemId"] as String
            )
            "OrderConfirmed" -> OrderConfirmed(
                orderId = p["orderId"] as String,
                confirmedAt = Instant.parse(p["confirmedAt"] as String)
            )
            "OrderCancelled" -> OrderCancelled(
                orderId = p["orderId"] as String,
                reason = p["reason"] as String,
                cancelledAt = Instant.parse(p["cancelledAt"] as String)
            )
            else -> throw IllegalArgumentException("Unknown event type: ${recorded.eventType}")
        }
        return event to recorded.streamVersion
    }
}
