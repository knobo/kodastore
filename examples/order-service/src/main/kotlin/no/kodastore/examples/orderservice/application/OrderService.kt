package no.kodastore.examples.orderservice.application

import no.kodastore.examples.orderservice.client.KodaStoreClient
import no.kodastore.examples.orderservice.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderService(private val store: KodaStoreClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun createOrder(customerId: String): OrderState {
        val orderId = UUID.randomUUID().toString().substring(0, 8)
        val streamId = "Order-$orderId"

        val event = OrderCreated(orderId = orderId, customerId = customerId)
        store.append(streamId, -1, listOf(EventMapper.toRequest(event)))

        log.info("Created order $orderId for customer $customerId")
        return loadOrder(orderId)
    }

    fun addItem(orderId: String, productName: String, quantity: Int, unitPrice: Double): OrderState {
        val current = loadOrder(orderId)
        require(current.status == OrderStatus.DRAFT) { "Cannot add items to ${current.status} order" }

        val itemId = UUID.randomUUID().toString().substring(0, 8)
        val event = ItemAdded(
            orderId = orderId,
            itemId = itemId,
            productName = productName,
            quantity = quantity,
            unitPrice = unitPrice
        )

        store.append("Order-$orderId", current.version, listOf(EventMapper.toRequest(event)))
        log.info("Added item $productName to order $orderId")
        return loadOrder(orderId)
    }

    fun removeItem(orderId: String, itemId: String): OrderState {
        val current = loadOrder(orderId)
        require(current.status == OrderStatus.DRAFT) { "Cannot remove items from ${current.status} order" }
        require(current.items.any { it.itemId == itemId }) { "Item $itemId not found in order" }

        val event = ItemRemoved(orderId = orderId, itemId = itemId)

        store.append("Order-$orderId", current.version, listOf(EventMapper.toRequest(event)))
        log.info("Removed item $itemId from order $orderId")
        return loadOrder(orderId)
    }

    fun confirmOrder(orderId: String): OrderState {
        val current = loadOrder(orderId)
        require(current.status == OrderStatus.DRAFT) { "Can only confirm DRAFT orders" }
        require(current.items.isNotEmpty()) { "Cannot confirm an empty order" }

        val event = OrderConfirmed(orderId = orderId)

        store.append("Order-$orderId", current.version, listOf(EventMapper.toRequest(event)))
        log.info("Confirmed order $orderId (total: ${current.totalAmount})")
        return loadOrder(orderId)
    }

    fun cancelOrder(orderId: String, reason: String): OrderState {
        val current = loadOrder(orderId)
        require(current.status != OrderStatus.CANCELLED) { "Order is already cancelled" }

        val event = OrderCancelled(orderId = orderId, reason = reason)

        store.append("Order-$orderId", current.version, listOf(EventMapper.toRequest(event)))
        log.info("Cancelled order $orderId: $reason")
        return loadOrder(orderId)
    }

    fun getOrder(orderId: String): OrderState = loadOrder(orderId)

    private fun loadOrder(orderId: String): OrderState {
        val stream = store.readStream("Order-$orderId")
        if (stream.events.isEmpty()) {
            throw OrderNotFoundException(orderId)
        }
        val domainEvents = stream.events.map { EventMapper.fromRecorded(it) }
        return OrderState.rebuild(domainEvents)
    }
}

class OrderNotFoundException(orderId: String) :
    RuntimeException("Order not found: $orderId")
