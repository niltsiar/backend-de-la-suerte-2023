package dev.niltsiar.luckybackend.service

import arrow.core.NonEmptyList
import dev.niltsiar.luckybackend.repo.OrderPersistence
import kotlinx.datetime.Instant

data class Order(
    val id: String,
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

    suspend fun createOrder()
    suspend fun getLastOrder(): Order
}

fun OrderService(
    orderPersistence: OrderPersistence,
): OrderService {
    return object : OrderService {

        override suspend fun createOrder() {

        }

        override suspend fun getLastOrder(): Order {
            return orderPersistence.getLastOrder()
        }
    }
}
