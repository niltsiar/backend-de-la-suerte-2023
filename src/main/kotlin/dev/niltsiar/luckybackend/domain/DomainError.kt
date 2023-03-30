package dev.niltsiar.luckybackend.domain

sealed interface DomainError

sealed interface ServiceError : DomainError
data class OrderAlreadyExists(val orderId: String) : ServiceError
data class MaxNumberOfOrders(val description: String) : ServiceError

sealed interface PersistenceError : DomainError {

    val description: String
}

data class OrderCreationError(override val description: String) : PersistenceError
data class OrderRetrievalError(override val description: String) : PersistenceError
data class OrderDispatchError(override val description: String) : PersistenceError
data class InvalidOrderError(override val description: String) : PersistenceError
data class OrderNotFound(val orderId: String, override val description: String) : PersistenceError, ServiceError

sealed interface NetworkError : DomainError
data class Unexpected(val description: String) : NetworkError
data class IllegalArgument(val description: String) : NetworkError
