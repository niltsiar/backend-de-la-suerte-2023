package dev.niltsiar.luckybackend

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import dev.niltsiar.luckybackend.repo.OrderPersistence
import dev.niltsiar.luckybackend.routes.orderRoutes
import dev.niltsiar.luckybackend.service.OrderService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json

fun main() = SuspendApp {
    resourceScope {
        server(Netty, port = 8080, host = "0.0.0.0", preWait = 0.seconds) {
            app()
        }
        awaitCancellation()
    }
}

fun Application.app() {
    configure()
    orderRoutes(OrderService(OrderPersistence(maxPendingOrders = 5)))
}

fun Application.configure() {
    install(ContentNegotiation) {
        json(
            Json {
                isLenient = true
            }
        )
    }
}
