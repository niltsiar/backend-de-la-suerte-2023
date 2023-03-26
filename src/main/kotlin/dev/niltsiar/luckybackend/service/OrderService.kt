package dev.niltsiar.luckybackend.service

import arrow.core.NonEmptyList
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import dev.niltsiar.luckybackend.domain.DomainError
import dev.niltsiar.luckybackend.domain.OrderAlreadyExists
import dev.niltsiar.luckybackend.domain.OrderNotFound
import dev.niltsiar.luckybackend.repo.OrderPersistence
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
): OrderService {
    return object : OrderService {

        context(EffectScope<DomainError>)
        override suspend fun createOrder(order: Order): Order {
            ensure(order.id == null) { OrderAlreadyExists(order.id!!) }
            return orderPersistence.saveOrder(order)
        }

        context(EffectScope<DomainError>)
        override suspend fun getOrders(): List<Order> {
            return orderPersistence.getOrders()
        }

        context(EffectScope<DomainError>)
        override suspend fun clearOrders() {
            return orderPersistence.clearOrders()
        }

        context(EffectScope<DomainError>)
        override suspend fun dispatchOrder(orderId: String) {
            return effect {
                orderPersistence.dispatchOrder(orderId)
            }.handleErrorWith {
                effect { shift(OrderNotFound(orderId)) }
            }.bind()
        }
    }
}
