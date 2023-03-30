package dev.niltsiar.luckybackend.repo

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.continuations.EffectScope
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.sequence
import arrow.core.toNonEmptyListOrNull
import dev.niltsiar.luckybackend.domain.InvalidOrderError
import dev.niltsiar.luckybackend.domain.OrderCreationError
import dev.niltsiar.luckybackend.domain.OrderDispatchError
import dev.niltsiar.luckybackend.domain.OrderNotFound
import dev.niltsiar.luckybackend.domain.OrderRetrievalError
import dev.niltsiar.luckybackend.domain.PersistenceError
import dev.niltsiar.luckybackend.service.Dish
import dev.niltsiar.luckybackend.service.Order
import java.io.File
import kotlinx.datetime.Instant

interface OrderPersistence {

    context(EffectScope<PersistenceError>)
    suspend fun saveOrder(order: Order): Order

    context(EffectScope<PersistenceError>)
    suspend fun getAllOrders(): List<Order>

    context(EffectScope<PersistenceError>)
    suspend fun clearOrders()

    context(EffectScope<PersistenceError>)
    suspend fun upsertOrder(order: Order)

    context(EffectScope<PersistenceError>)
    suspend fun removeOrder(order: Order)
}

fun OrderPersistence(): OrderPersistence {
    return object : OrderPersistence {

        private val STORAGE_FILE = "orders.menu"

        context(EffectScope<PersistenceError>)
        override suspend fun saveOrder(order: Order): Order {
            ensureNotNull(order.id) { InvalidOrderError("orderId cannot be null") }
            return Either.catch {
                val file = File(STORAGE_FILE)
                file.appendText("${order.serialize()}${System.lineSeparator()}")
                order
            }.mapLeft { e ->
                OrderCreationError(e.message.orEmpty())
            }.bind()
        }

        context(EffectScope<PersistenceError>)
        override suspend fun getAllOrders(): List<Order> {
            return Either.catch {
                val file = File(STORAGE_FILE)
                file.readLines().map { serializedOrder -> Order.deserialize(serializedOrder) }
                    .sequence()
                    .bind()
            }.mapLeft {
                OrderRetrievalError("Error loading orders")
            }.bind()
        }

        context(EffectScope<PersistenceError>)
        override suspend fun clearOrders() {
            return Either.catch {
                val file = File(STORAGE_FILE)
                file.delete()
                Unit
            }.mapLeft {
                OrderDispatchError("Error dispatching orders")
            }.bind()
        }

        context(EffectScope<PersistenceError>)
        override suspend fun upsertOrder(order: Order) {
            ensureNotNull(order.id) { InvalidOrderError("Order id cannot be null") }
            val storedOrders = getAllOrders()
                .filterNot { storedOrder -> storedOrder.id.isNullOrBlank() }
                .associateBy { storedOrder -> storedOrder.id!! }
                .toMutableMap()

            storedOrders[order.id] = order

            storeOrders(storedOrders.values.toList())
        }

        context(EffectScope<PersistenceError>)
        override suspend fun removeOrder(order: Order) {
            ensureNotNull(order.id) { InvalidOrderError("Order id cannot be null") }
            val storedOrders = getAllOrders()
                .filterNot { storedOrder -> storedOrder.id.isNullOrBlank() }
                .associateBy { storedOrder -> storedOrder.id!! }
                .toMutableMap()

            ensure(storedOrders.containsKey(order.id)) {
                OrderNotFound(order.id, "Order with id=${order.id} was not found")
            }

            storedOrders.remove(order.id)

            storeOrders(storedOrders.values.toList())
        }

        context(EffectScope<PersistenceError>)
        private suspend fun storeOrders(orders: List<Order>) {
            return Either.catch {
                val file = File(STORAGE_FILE)
                file.delete()
                orders.forEach { orderToStore ->
                    file.appendText("${orderToStore.serialize()}${System.lineSeparator()}")
                }
            }.mapLeft { e ->
                OrderCreationError(e.message.orEmpty())
            }.bind()
        }
    }
}

private const val ORDER_FIELD_SEPARATOR = "✂️"
private const val DISH_FIELD_SEPARATOR = "🥢"
private const val DISH_SEPARATOR = "🥡"
private const val ID_TAG = "🆔"
private const val CREATED_AT_TAG = "⏱"
private const val DISPATCHED_AT_TAG = "⏲"
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
            dispatchedAt?.let { appendOrderField(DISPATCHED_AT_TAG, dispatchedAt.toString()) }
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
        val dispatchedAt = parts[DISPATCHED_AT_TAG]
        val serializedDished = parts[DISHES_TAG] ?: shift(OrderRetrievalError("Dishes cannot be null"))
        val dishes = Dish.deserializeDishes(serializedDished).bind()

        Either.catch {
            Order(
                id = id,
                createdAt = Instant.parse(createdAt),
                dispatchedAt = dispatchedAt?.let { Instant.parse(it) },
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
