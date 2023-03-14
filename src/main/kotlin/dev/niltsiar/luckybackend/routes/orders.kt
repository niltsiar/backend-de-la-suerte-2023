package dev.niltsiar.luckybackend.routes

import dev.niltsiar.luckybackend.service.OrderService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.orderRoutes(
    orderService: OrderService,
) {
    routing {
        route("/orders") {
            post {
                orderService.createOrder()
                call.respond(HttpStatusCode.Created)
            }

            get {
                call.respond(HttpStatusCode.OK, orderService.getLastOrder().toString())
            }
        }
    }
}
