package com.chaddy50.concerttracker.data.api

object ApiErrorType {
    enum class Type {
        NETWORK,
        TIMEOUT,
        SERVER,
        CLIENT,
        CONFLICT,
        UNKNOWN;

        fun toUserMessage(): String = when (this) {
            NETWORK  -> "Can't reach the server. Check your connection and try again."
            TIMEOUT  -> "The request timed out. Try again."
            SERVER   -> "Something went wrong on the server. Try again in a moment."
            CLIENT   -> "The server rejected this request."
            CONFLICT -> "This record already exists."
            UNKNOWN  -> "An unexpected error occurred."
        }
    }
}
