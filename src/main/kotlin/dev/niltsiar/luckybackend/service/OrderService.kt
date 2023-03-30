package dev.niltsiar.luckybackend.service

import arrow.core.NonEmptyList
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import dev.niltsiar.luckybackend.domain.DomainError
import dev.niltsiar.luckybackend.domain.MaxNumberOfOrders
import dev.niltsiar.luckybackend.domain.OrderAlreadyExists
import dev.niltsiar.luckybackend.domain.OrderNotFound
import dev.niltsiar.luckybackend.repo.OrderPersistence
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Order(
    val id: String?,
    val table: Int,
    val createdAt: Instant,
    val dispatchedAt: Instant?,
    val dishes: NonEmptyList<Dish>,
) {

    companion object
}

data class Dish(
    val name: String,
    val quantity: Int,
) {

    companion object
}

interface OrderService {

    context(EffectScope<DomainError>)
    suspend fun createOrder(order: Order): Order

    context(EffectScope<DomainError>)
    suspend fun getOrders(): List<Order>

    context(EffectScope<DomainError>)
    suspend fun clearOrders()

    context(EffectScope<DomainError>)
    suspend fun dispatchOrder(orderId: String)
}

fun OrderService(
    orderPersistence: OrderPersistence,
    maxPendingOrders: Int,
): OrderService {
    return object : OrderService {

        private val orders = mutableMapOf<String, Order>()
        private val orderComparator = Comparator<Order> { o1, o2 ->
            if (o1.containsSpecialZombie() == o2.containsSpecialZombie()) {
                o1.createdAt.compareTo(o2.createdAt)
            } else if (o1.containsSpecialZombie()) {
                -1
            } else {
                1
            }
        }

        init {
            runBlocking {
                effect {
                    orderPersistence.getAllOrders()
                        .filterNot { order -> order.id.isNullOrBlank() }
                        .filter { order -> order.dispatchedAt == null }
                        .forEach { order ->
                            orders[order.id!!] = order
                        }
                }.orNull() // We need to execute the Effect
            }
        }

        context(EffectScope<DomainError>)
        override suspend fun createOrder(order: Order): Order {
            ensure(orders.size < maxPendingOrders) {
                MaxNumberOfOrders("No more than $maxPendingOrders pending orders allowed")
            }
            ensure(order.id == null) { OrderAlreadyExists(order.id!!) }
            val orderId = createUniqueOrderId()
            val createdOrder = orderPersistence.saveOrder(order.copy(id = orderId))
            orders[createdOrder.id!!] = createdOrder
            return createdOrder
        }

        context(EffectScope<DomainError>)
        override suspend fun getOrders(): List<Order> {
            return orders.values.sortedWith(orderComparator)
        }

        context(EffectScope<DomainError>)
        override suspend fun clearOrders() {
            return orderPersistence.clearOrders()
        }

        context(EffectScope<DomainError>)
        override suspend fun dispatchOrder(orderId: String) {
            val orderToDispatch = orders[orderId] ?: shift(OrderNotFound(orderId, "Order with id=$orderId was not found"))
            val dispatchedOrder = orderToDispatch.copy(dispatchedAt = Clock.System.now())

            orderPersistence.upsertOrder(dispatchedOrder)
            orders.remove(dispatchedOrder.id)
        }

        private fun createUniqueOrderId(): String {
            return UUID.randomUUID().toString()
        }
    }
}

private const val SPECIAL_ZOMBIE = "ESPECIAL ZOMBIE"
private val Dish.isSpecialZombie: Boolean
    get() = name == SPECIAL_ZOMBIE

private fun Order.containsSpecialZombie(): Boolean = dishes.any { dish -> dish.isSpecialZombie }
