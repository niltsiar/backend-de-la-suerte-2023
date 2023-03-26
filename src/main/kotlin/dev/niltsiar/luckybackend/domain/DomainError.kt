package dev.niltsiar.luckybackend.domain

sealed interface DomainError

sealed interface ServiceError : DomainError
data class OrderAlreadyExists(val orderId: String) : ServiceError

sealed interface PersistenceError : DomainError {

    val description: String
}

data class OrderCreationError(override val description: String) : PersistenceError
data class OrderRetrievalError(override val description: String) : PersistenceError
data class MaxNumberOfOrders(override val description: String) : PersistenceError

sealed interface NetworkError : DomainError
data class Unexpected(val description: String) : NetworkError
