package dev.niltsiar.luckybackend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.orderRoutes() {
    routing {
        route("/orders") {
            post {
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
