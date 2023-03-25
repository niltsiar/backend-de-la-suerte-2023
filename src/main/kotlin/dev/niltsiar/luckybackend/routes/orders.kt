package dev.niltsiar.luckybackend.routes

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.toNonEmptyListOrNull
import dev.niltsiar.luckybackend.service.Dish
import dev.niltsiar.luckybackend.service.Order
import dev.niltsiar.luckybackend.service.OrderService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

fun Application.orderRoutes(
    orderService: OrderService,
) {
    routing {
        route("/orders") {
            post {
                val res = either {
                    val remoteOrder = receiveCatching<RemoteOrder>().bind()
                    Either.catch {
                        remoteOrder.asOrder()
                    }.bind()
                    remoteOrder
                }
                respond(HttpStatusCode.OK, res)
            }

            get {
                call.respond(HttpStatusCode.OK, orderService.getLastOrder().toString())
            }
        }
    }
}

private suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.receiveCatching(): Either<Throwable, A> {
    return Either.catch {
        call.receive<A>()
    }
}

@Serializable
data class RemoteDish(
    val name: String,
    val quantity: Int,
)

@Serializable
data class RemoteOrder(
    val table: Int,
    val createdAt: Instant = Clock.System.now(),
    val dishes: List<RemoteDish>,
)

private fun RemoteDish.asDish() = Dish(
    name = name,
    quantity = quantity,
)

private fun RemoteOrder.asOrder() = Order(
    id = UUID.randomUUID().toString(),
    table = table,
    createdAt = createdAt,
    dishes = dishes.map(RemoteDish::asDish).toNonEmptyListOrNull() ?: throw Throwable("Dishes cannot be an empty list")
)
