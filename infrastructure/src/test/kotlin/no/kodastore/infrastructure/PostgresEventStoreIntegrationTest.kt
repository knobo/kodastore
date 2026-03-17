package no.kodastore.infrastructure

import no.kodastore.domain.model.*
import no.kodastore.domain.port.EventStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
class PostgresEventStoreIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")
            .withDatabaseName("kodastore_test")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    lateinit var eventStore: EventStore

    @Test
    fun `append and read stream`() {
        val streamId = StreamId("Order-001")
        val events = listOf(
            NewEvent("OrderCreated", mapOf("orderId" to "001", "amount" to 100)),
            NewEvent("ItemAdded", mapOf("itemId" to "A", "quantity" to 2))
        )

        val recorded = eventStore.append(streamId, ExpectedVersion.NoStream, events)

        assertEquals(2, recorded.size)
        assertEquals(1, recorded[0].streamVersion.value)
        assertEquals(2, recorded[1].streamVersion.value)
        assertEquals("OrderCreated", recorded[0].eventType)
        assertEquals("ItemAdded", recorded[1].eventType)

        val state = eventStore.readStream(streamId)
        assertEquals(2, state.version.value)
        assertEquals(2, state.events.size)
        assertEquals("Order-001", state.streamId.value)
    }

    @Test
    fun `optimistic concurrency conflict on NoStream`() {
        val streamId = StreamId("Order-002")
        eventStore.append(
            streamId,
            ExpectedVersion.NoStream,
            listOf(NewEvent("OrderCreated", mapOf("orderId" to "002")))
        )

        assertThrows<OptimisticConcurrencyException> {
            eventStore.append(
                streamId,
                ExpectedVersion.NoStream,
                listOf(NewEvent("OrderCreated", mapOf("orderId" to "002-dup")))
            )
        }
    }

    @Test
    fun `optimistic concurrency conflict on Exact version`() {
        val streamId = StreamId("Order-003")
        eventStore.append(
            streamId,
            ExpectedVersion.NoStream,
            listOf(NewEvent("OrderCreated", mapOf("orderId" to "003")))
        )

        assertThrows<OptimisticConcurrencyException> {
            eventStore.append(
                streamId,
                ExpectedVersion.Exact(StreamVersion(0)),
                listOf(NewEvent("ItemAdded", mapOf("itemId" to "B")))
            )
        }

        val recorded = eventStore.append(
            streamId,
            ExpectedVersion.Exact(StreamVersion(1)),
            listOf(NewEvent("ItemAdded", mapOf("itemId" to "B")))
        )
        assertEquals(2, recorded[0].streamVersion.value)
    }

    @Test
    fun `read all events in global order`() {
        val stream1 = StreamId("Product-001")
        val stream2 = StreamId("Product-002")

        eventStore.append(
            stream1,
            ExpectedVersion.NoStream,
            listOf(NewEvent("ProductCreated", mapOf("name" to "Widget")))
        )
        eventStore.append(
            stream2,
            ExpectedVersion.NoStream,
            listOf(NewEvent("ProductCreated", mapOf("name" to "Gadget")))
        )

        val allEvents = eventStore.readAll(GlobalOffset.START, 100)
        assertTrue(allEvents.size >= 2)

        val offsets = allEvents.map { it.globalOffset.value }
        assertEquals(offsets.sorted(), offsets)
    }

    @Test
    fun `read events by category`() {
        val orderId = "Cat-${System.nanoTime()}"
        val stream1 = StreamId("Invoice-$orderId-1")
        val stream2 = StreamId("Invoice-$orderId-2")
        val stream3 = StreamId("Payment-$orderId-1")

        eventStore.append(
            stream1,
            ExpectedVersion.NoStream,
            listOf(NewEvent("InvoiceCreated", mapOf("id" to "1")))
        )
        eventStore.append(
            stream2,
            ExpectedVersion.NoStream,
            listOf(NewEvent("InvoiceCreated", mapOf("id" to "2")))
        )
        eventStore.append(
            stream3,
            ExpectedVersion.NoStream,
            listOf(NewEvent("PaymentReceived", mapOf("id" to "1")))
        )

        val invoiceEvents = eventStore.readCategory("Invoice", GlobalOffset.START, 100)
        assertTrue(invoiceEvents.all { it.streamId.value.startsWith("Invoice-") })
        assertTrue(invoiceEvents.size >= 2)
    }

    @Test
    fun `append with metadata`() {
        val streamId = StreamId("Order-meta-001")
        val events = listOf(
            NewEvent(
                "OrderCreated",
                mapOf("orderId" to "meta-001"),
                EventMetadata(
                    correlationId = "corr-123",
                    causalityId = "cause-456",
                    userId = "user-789"
                )
            )
        )

        val recorded = eventStore.append(streamId, ExpectedVersion.NoStream, events)
        assertEquals("corr-123", recorded[0].metadata.correlationId)
        assertEquals("cause-456", recorded[0].metadata.causalityId)
        assertEquals("user-789", recorded[0].metadata.userId)

        val state = eventStore.readStream(streamId)
        assertEquals("corr-123", state.events[0].metadata.correlationId)
    }
}
