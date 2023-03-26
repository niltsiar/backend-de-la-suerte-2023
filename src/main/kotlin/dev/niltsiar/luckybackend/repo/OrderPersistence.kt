package dev.niltsiar.luckybackend.repo

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.continuations.either
import arrow.core.left
import arrow.core.right
import arrow.core.sequence
import arrow.core.toNonEmptyListOrNull
import dev.niltsiar.luckybackend.domain.MaxNumberOfOrders
import dev.niltsiar.luckybackend.domain.OrderCreationError
import dev.niltsiar.luckybackend.domain.OrderRetrievalError
import dev.niltsiar.luckybackend.domain.PersistenceError
import dev.niltsiar.luckybackend.service.Dish
import dev.niltsiar.luckybackend.service.Order
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant

interface OrderPersistence {

    suspend fun saveOrder(order: Order): Either<PersistenceError, Order>
    suspend fun getOrders(): Either<PersistenceError, List<Order>>
}

fun OrderPersistence(maxPendingOrders: Int): OrderPersistence {
    return object : OrderPersistence {

        private val orders = mutableListOf<Order>()
        private val orderComparator = Comparator<Order> { o1, o2 -> o1.createdAt.compareTo(o2.createdAt) }

        private val STORAGE_FILE = "orders.menu"

        init {
            runBlocking {
                val file = File(STORAGE_FILE)
                val loadedOrders = file.readLines().map { serializedOrder -> Order.deserialize(serializedOrder) }.sequence()
                loadedOrders
                    .onRight {
                        orders.addAll(it)
                        orders.sortWith(orderComparator)
                    }
                    .onLeft {
                        file.delete()
                    }
            }
        }

        override suspend fun saveOrder(order: Order): Either<PersistenceError, Order> {
            if (orders.size >= maxPendingOrders) {
                return MaxNumberOfOrders("No more than $maxPendingOrders pending orders allowed").left()
            }
            return Either.catch {
                val createdOrder = order.copy(id = UUID.randomUUID().toString())
                val file = File(STORAGE_FILE)
                file.appendText("${createdOrder.serialize()}${System.lineSeparator()}")
                createdOrder
            }.onRight { createdOrder ->
                orders.add(createdOrder)
                orders.sortWith(orderComparator)
            }.mapLeft { e ->
                OrderCreationError(e.message.orEmpty())
            }
        }

        override suspend fun getOrders(): Either<PersistenceError, List<Order>> {
            return orders.right()
        }
    }
}

private const val ORDER_FIELD_SEPARATOR = "✂️"
private const val DISH_FIELD_SEPARATOR = "🥢"
private const val DISH_SEPARATOR = "🥡"
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
            val serializedDishes = dishes.joinToString(separator = DISH_SEPARATOR) { it.serialize() }
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

private suspend fun Order.Companion.deserialize(serializedOrder: String): Either<PersistenceError, Order> {
    return either {
        val parts = Either.catch {
            serializedOrder.split(ORDER_FIELD_SEPARATOR).associate { field ->
                val (tag, value) = field.split("=", limit = 2)
                tag to value
            }
        }.mapLeft {
            OrderRetrievalError("Error loading order")
        }.bind()

        val id = parts[ID_TAG] ?: shift(OrderRetrievalError("Order id cannot be null"))
        val table = parts[TABLE_TAG] ?: shift(OrderRetrievalError("Table cannot be null"))
        val createdAt = parts[CREATED_AT_TAG] ?: shift(OrderRetrievalError("Creation date cannot be null"))
        val serializedDished = parts[DISHES_TAG] ?: shift(OrderRetrievalError("Dishes cannot be null"))
        val dishes = Dish.deserializeDishes(serializedDished).bind()

        Either.catch {
            Order(
                id = id,
                createdAt = Instant.parse(createdAt),
                table = table.toInt(),
                dishes = dishes,
            )
        }.mapLeft {
            OrderRetrievalError("Error loading order")
        }.bind()
    }
}

private suspend fun Dish.Companion.deserializeDishes(serializedDishes: String): Either<PersistenceError, NonEmptyList<Dish>> {
    return either {
        val dishes = serializedDishes.split(DISH_SEPARATOR)
        dishes.map { Dish.deserializeDish(it) }
            .sequence()
            .map { it.toNonEmptyListOrNull() ?: shift(OrderRetrievalError("Dishes cannot be an empty list")) }
            .bind()
    }
}

private suspend fun Dish.Companion.deserializeDish(serializedDish: String): Either<PersistenceError, Dish> {
    return either {
        val parts = Either.catch {
            serializedDish.split(DISH_FIELD_SEPARATOR).associate { field ->
                val (tag, value) = field.split("=")
                tag to value
            }
        }.mapLeft {
            OrderRetrievalError("Error loading dish")
        }.bind()

        val name = parts[DISH_NAME_TAG] ?: shift(OrderRetrievalError("Dish name should not be null"))
        val quantity = parts[QUANTITY_TAG] ?: shift(OrderRetrievalError("Dish quantity should not be null"))

        Either.catch {
            Dish(
                name = name,
                quantity = quantity.toInt()
            )
        }.mapLeft {
            OrderRetrievalError("Error loading dish")
        }.bind()
    }
}
