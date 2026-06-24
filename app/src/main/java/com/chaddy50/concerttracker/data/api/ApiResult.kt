package com.chaddy50.concerttracker.data.api

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val errorType: ApiErrorType.Type) : ApiResult<Nothing>()
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onError(action: (ApiErrorType.Type) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) action(errorType)
    return this
}
