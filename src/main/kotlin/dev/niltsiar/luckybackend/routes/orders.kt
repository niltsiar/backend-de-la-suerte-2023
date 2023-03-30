package dev.niltsiar.luckybackend.routes

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import dev.niltsiar.luckybackend.domain.DomainError
import dev.niltsiar.luckybackend.domain.IllegalArgument
import dev.niltsiar.luckybackend.domain.Unexpected
import dev.niltsiar.luckybackend.service.Dish
import dev.niltsiar.luckybackend.service.Order
import dev.niltsiar.luckybackend.service.OrderService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

context(Application, OrderService)
fun orderRoutes() {
    routing {
        route("/orders") {
            post {
                val res = either<DomainError, RemoteOrder> {
                    val remoteOrder = receiveCatching<RemoteOrder>().bind()
                    val order = remoteOrder.asOrder().bind()
                    createOrder(order).asRemoteOrder()
                }
                respond(HttpStatusCode.Created, res)
            }

            get {
                val res = either {
                    getOrders().map(Order::asRemoteOrder)
                }
                respond(HttpStatusCode.OK, res)
            }
        }
        get("/clear") {
            val res = either {
                clearOrders()
            }
            respond(HttpStatusCode.OK, res)
        }

        get("/dispatch/{orderId}") {
            val res = either {
                val orderId = call.parameters["orderId"]
                ensureNotNull(orderId) { IllegalArgument("Order Id cannot be null") }
                dispatchOrder(orderId)
            }
            respond(HttpStatusCode.OK, res)
        }
    }
}

private suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.receiveCatching(): Either<DomainError, A> {
    return Either.catch {
        call.receive<A>()
    }.mapLeft { e ->
        Unexpected(e.message ?: "Received malformed JSON for ${A::class.simpleName}")
    }
}

@Serializable
data class RemoteDish(
    val name: String,
    val quantity: Int,
)

@Serializable
data class RemoteOrder(
    val id: String? = null,
    val table: Int,
    val createdAt: Instant = Clock.System.now(),
    val dispatchedAt: Instant? = null,
    val dishes: List<RemoteDish>,
)

private fun RemoteDish.asDish() = Dish(
    name = name,
    quantity = quantity,
)

private fun RemoteOrder.asOrder(): Either<DomainError, Order> {
    return Either.catch {
        Order(
            id = id,
            table = table,
            createdAt = createdAt,
            dispatchedAt = dispatchedAt,
            dishes = dishes.map(RemoteDish::asDish).toNonEmptyListOrNull() ?: throw Throwable()
        )
    }.mapLeft {
        Unexpected("Dishes cannot be an empty list")
    }
}

private fun Dish.asRemoteDish() = RemoteDish(
    name = name,
    quantity = quantity,
)

private fun Order.asRemoteOrder() = RemoteOrder(
    id = id,
    table = table,
    createdAt = createdAt,
    dishes = dishes.map(Dish::asRemoteDish)
)
