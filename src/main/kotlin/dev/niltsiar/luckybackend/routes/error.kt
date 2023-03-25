package dev.niltsiar.luckybackend.routes

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext

suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.respond(status: HttpStatusCode, either: Either<Throwable, A>): Unit =
    when (either) {
        is Either.Left -> respond(either.value)
        is Either.Right -> call.respond(status, either.value)
    }

suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: Throwable): Unit =
    call.respond(HttpStatusCode.UnprocessableEntity, error.message.orEmpty())
