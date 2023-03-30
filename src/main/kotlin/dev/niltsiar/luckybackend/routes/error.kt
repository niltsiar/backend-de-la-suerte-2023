package dev.niltsiar.luckybackend.routes

import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import dev.niltsiar.luckybackend.KtorCtx
import dev.niltsiar.luckybackend.domain.DomainError
import dev.niltsiar.luckybackend.domain.IllegalArgument
import dev.niltsiar.luckybackend.domain.InvalidOrderError
import dev.niltsiar.luckybackend.domain.MaxNumberOfOrders
import dev.niltsiar.luckybackend.domain.NetworkError
import dev.niltsiar.luckybackend.domain.OrderAlreadyExists
import dev.niltsiar.luckybackend.domain.OrderCreationError
import dev.niltsiar.luckybackend.domain.OrderDispatchError
import dev.niltsiar.luckybackend.domain.OrderNotFound
import dev.niltsiar.luckybackend.domain.OrderRetrievalError
import dev.niltsiar.luckybackend.domain.PersistenceError
import dev.niltsiar.luckybackend.domain.ServiceError
import dev.niltsiar.luckybackend.domain.Unexpected
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond

context(KtorCtx)
suspend inline fun <reified A : Any> conduit(
    status: HttpStatusCode,
    crossinline block: suspend context(EffectScope<DomainError>) () -> A,
): Unit = effect {
    block(this)
}.fold(
    recover = { respond(it) },
    transform = { value ->
        if (value == Unit) {
            call.respond(status)
        } else {
            call.respond(status, value)
        }
    }
)

suspend fun KtorCtx.respond(error: DomainError): Unit =
    when (error) {
        is ServiceError -> respond(error)
        is PersistenceError -> respond(error)
        is NetworkError -> respond(error)
    }

private suspend fun KtorCtx.respond(error: ServiceError): Unit =
    when (error) {
        is OrderAlreadyExists -> call.respond(HttpStatusCode.Conflict)
        is OrderNotFound -> call.respond(HttpStatusCode.NotFound)
        is MaxNumberOfOrders -> call.respond(HttpStatusCode.InsufficientStorage, error.description)
    }

private suspend fun KtorCtx.respond(error: PersistenceError): Unit =
    when (error) {
        is OrderCreationError -> unprocessable(error.description)
        is OrderRetrievalError -> unprocessable(error.description)
        is OrderDispatchError -> call.respond(HttpStatusCode.InternalServerError, error.description)
        is InvalidOrderError -> call.respond(HttpStatusCode.BadRequest)
        is OrderNotFound -> call.respond(HttpStatusCode.NotFound)
    }

private suspend fun KtorCtx.respond(error: NetworkError): Unit =
    when (error) {
        is IllegalArgument -> call.respond(HttpStatusCode.BadRequest, error.description)
        is Unexpected -> call.respond(HttpStatusCode.UnprocessableEntity, error.description)
    }

private suspend fun KtorCtx.unprocessable(error: String): Unit =
    call.respond(HttpStatusCode.UnprocessableEntity, error)
