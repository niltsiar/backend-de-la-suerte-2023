package dev.niltsiar.luckybackend.repo

import arrow.core.Either
import arrow.core.toNonEmptyListOrNull
import dev.niltsiar.luckybackend.domain.OrderCreationError
import dev.niltsiar.luckybackend.domain.PersistenceError
import dev.niltsiar.luckybackend.service.Dish
import dev.niltsiar.luckybackend.service.Order
import java.io.File
import java.util.UUID
import kotlinx.datetime.Instant

interface OrderPersistence {

    suspend fun saveOrder(order: Order): Either<PersistenceError, Order>
    suspend fun getLastOrder(): Order
}

fun OrderPersistence(): OrderPersistence {
    return object : OrderPersistence {

        private val STORAGE_FILE = "orders.menu"

        override suspend fun saveOrder(order: Order): Either<PersistenceError, Order> {
            return Either.catch {
                val createdOrder = order.copy(id = UUID.randomUUID().toString())
                val file = File(STORAGE_FILE)
                file.appendText("${createdOrder.serialize()}${System.lineSeparator()}")
                createdOrder
            }.mapLeft { e ->
                OrderCreationError(e.message.orEmpty())
            }
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
        table = 0,
        dishes = emptyList<Dish>().toNonEmptyListOrNull()!!,
    )
}
