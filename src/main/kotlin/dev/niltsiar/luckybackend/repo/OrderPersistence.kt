package dev.niltsiar.luckybackend.repo

import dev.niltsiar.luckybackend.service.Order
import java.io.File
import java.time.Instant

interface OrderPersistence {

    suspend fun saveOrder(order: Order)
    suspend fun getLastOrder(): Order
}

fun OrderPersistence(): OrderPersistence {
    return object : OrderPersistence {

        private val STORAGE_FILE = "orders.menu"

        override suspend fun saveOrder(order: Order) {
            val file = File(STORAGE_FILE)
            file.appendText("${order.serialize()}${System.lineSeparator()}")
        }

        override suspend fun getLastOrder(): Order {
            val file = File(STORAGE_FILE)
            return Order.deserialize(file.readLines().last())
        }
    }
}

private const val FIELD_SEPARATOR = "✂️"
private const val ID_TAG = "🆔"
private const val CREATED_AT_TAG = "⏱"
private fun Order.serialize(): String {
    return StringBuilder()
        .append("$ID_TAG=").append(id).append(FIELD_SEPARATOR)
        .append("$CREATED_AT_TAG=").append(createdAt)
        .toString()

}

private fun Order.Companion.deserialize(serializedOrder: String): Order {
    val parts = serializedOrder.split(FIELD_SEPARATOR).associate { field ->
        val (tag, value) = field.split("=")
        tag to value
    }

    val id = parts[ID_TAG] ?: throw IllegalArgumentException()
    val createdAt = parts[CREATED_AT_TAG] ?: throw IllegalArgumentException()
    return Order(
        id = id,
        createdAt = Instant.parse(createdAt),
    )
}
