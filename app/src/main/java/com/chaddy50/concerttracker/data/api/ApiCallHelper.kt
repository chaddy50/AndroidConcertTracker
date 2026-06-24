package com.chaddy50.concerttracker.data.api

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        when (e.code()) {
            in 500..599 -> ApiResult.Error(ApiErrorType.Type.SERVER)
            409 -> ApiResult.Error(ApiErrorType.Type.CONFLICT)
            else -> ApiResult.Error(ApiErrorType.Type.CLIENT)
        }
    } catch (e: SocketTimeoutException) {
        ApiResult.Error(ApiErrorType.Type.TIMEOUT)
    } catch (e: UnknownHostException) {
        ApiResult.Error(ApiErrorType.Type.NETWORK)
    } catch (e: ConnectException) {
        ApiResult.Error(ApiErrorType.Type.NETWORK)
    } catch (e: IOException) {
        ApiResult.Error(ApiErrorType.Type.NETWORK)
    } catch (e: Exception) {
        ApiResult.Error(ApiErrorType.Type.UNKNOWN)
    }
}
