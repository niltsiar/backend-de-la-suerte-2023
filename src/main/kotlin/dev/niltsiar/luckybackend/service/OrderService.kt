package dev.niltsiar.luckybackend.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.continuations.either
import dev.niltsiar.luckybackend.domain.DomainError
import dev.niltsiar.luckybackend.domain.OrderAlreadyExists
import dev.niltsiar.luckybackend.repo.OrderPersistence
import kotlinx.datetime.Instant

data class Order(
    val id: String?,
    val table: Int,
    val createdAt: Instant,
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

    suspend fun createOrder(order: Order): Either<DomainError, Order>
    suspend fun getOrders(): Either<DomainError, List<Order>>
}

fun OrderService(
    orderPersistence: OrderPersistence,
): OrderService {
    return object : OrderService {

        override suspend fun createOrder(order: Order): Either<DomainError, Order> {
            return either {
                ensure(order.id == null) { OrderAlreadyExists(order.id!!) }
                orderPersistence.saveOrder(order).bind()
            }
        }

        override suspend fun getOrders(): Either<DomainError, List<Order>> {
            return orderPersistence.getOrders()
        }
    }
}
