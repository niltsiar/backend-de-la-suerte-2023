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

private const val ORDER_FIELD_SEPARATOR = "✂️"
private const val DISH_FIELD_SEPARATOR = "🥢"
private const val ID_TAG = "🆔"
private const val CREATED_AT_TAG = "⏱"
private const val TABLE_TAG = "🪑"
private const val DISHES_TAG = "📃"
private const val DISH_NAME_TAG = "🍽"
private const val QUANTITY_TAG = "🔢"

private fun Order.serialize(): String {
    return StringBuilder()
        .apply {
            appendOrderField(ID_TAG, id.toString())
            appendOrderField(TABLE_TAG, table.toString())
            appendOrderField(CREATED_AT_TAG, createdAt.toString())
            val serializedDishes = dishes.joinToString { it.serialize() }
            appendOrderField(DISHES_TAG, serializedDishes)
        }
        .removePrefix(ORDER_FIELD_SEPARATOR)
        .toString()
}

private fun Dish.serialize(): String {
    return StringBuilder()
        .apply {
            appendDishField(DISH_NAME_TAG, name)
            appendDishField(QUANTITY_TAG, quantity.toString())
        }
        .removePrefix(DISH_FIELD_SEPARATOR)
        .toString()
}

private fun StringBuilder.appendOrderField(tag: String, value: String): StringBuilder {
    return appendField(tag, value, ORDER_FIELD_SEPARATOR)
}

private fun StringBuilder.appendDishField(tag: String, value: String): StringBuilder {
    return appendField(tag, value, DISH_FIELD_SEPARATOR)
}

private fun StringBuilder.appendField(tag: String, value: String, fieldSeparator: String): StringBuilder {
    return apply {
        append(fieldSeparator)
        append("$tag=")
        append(value)
    }
}

private fun Order.Companion.deserialize(serializedOrder: String): Order {
    val parts = serializedOrder.split(ORDER_FIELD_SEPARATOR).associate { field ->
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
