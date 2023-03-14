package dev.niltsiar.luckybackend

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import io.ktor.server.netty.Netty
import kotlinx.coroutines.awaitCancellation

fun main() = SuspendApp {
    resourceScope {
        server(Netty, port = 8080, host = "0.0.0.0")
        awaitCancellation()
    }
}
