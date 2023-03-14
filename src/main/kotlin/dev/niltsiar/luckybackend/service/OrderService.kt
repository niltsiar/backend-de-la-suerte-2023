package dev.niltsiar.luckybackend.service

import java.time.Instant
import java.util.UUID

data class Order(
    val id: String,
    val createdAt: Instant,
)

interface OrderService {

    suspend fun createOrder()
    suspend fun getLastOrder(): Order
}

fun OrderService(): OrderService {
    return object : OrderService {

        private var lastOrder: Order? = null

        override suspend fun createOrder() {
            lastOrder = Order(
                id = UUID.randomUUID().toString(),
                createdAt = Instant.now(),
            )
        }

        override suspend fun getLastOrder(): Order {
            return lastOrder!!
        }
    }
}
