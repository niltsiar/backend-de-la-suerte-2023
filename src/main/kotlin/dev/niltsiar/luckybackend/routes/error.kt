package dev.niltsiar.luckybackend.routes

import arrow.core.Either
import dev.niltsiar.luckybackend.domain.DomainError
import dev.niltsiar.luckybackend.domain.MaxNumberOfOrders
import dev.niltsiar.luckybackend.domain.OrderCreationError
import dev.niltsiar.luckybackend.domain.OrderRetrievalError
import dev.niltsiar.luckybackend.domain.PersistenceError
import dev.niltsiar.luckybackend.domain.ServiceError
import dev.niltsiar.luckybackend.domain.Unexpected
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext

suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.respond(status: HttpStatusCode, either: Either<DomainError, A>): Unit =
    when (either) {
        is Either.Left -> respond(either.value)
        is Either.Right -> call.respond(status, either.value)
    }

suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: DomainError): Unit =
    when (error) {
        is ServiceError -> call.respond(HttpStatusCode.Conflict)
        is PersistenceError -> respond(error)
        is Unexpected -> unprocessable(error.description)
    }

private suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: PersistenceError): Unit =
    when (error) {
        is MaxNumberOfOrders -> call.respond(HttpStatusCode.InsufficientStorage, error.description)
        is OrderCreationError -> unprocessable(error.description)
        is OrderRetrievalError -> unprocessable(error.description)
    }

private suspend fun PipelineContext<Unit, ApplicationCall>.unprocessable(error: String): Unit =
    call.respond(HttpStatusCode.UnprocessableEntity, error)
