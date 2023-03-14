package dev.niltsiar.luckybackend.service

import dev.niltsiar.luckybackend.repo.OrderPersistence
import java.time.Instant
import java.util.UUID

data class Order(
    val id: String,
    val createdAt: Instant,
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
            val newOrder = Order(
                id = UUID.randomUUID().toString(),
                createdAt = Instant.now(),
            )
            orderPersistence.saveOrder(newOrder)
        }

        override suspend fun getLastOrder(): Order {
            return orderPersistence.getLastOrder()
        }
    }
}
