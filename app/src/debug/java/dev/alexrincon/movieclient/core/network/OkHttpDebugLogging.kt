package dev.alexrincon.movieclient.core.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

internal fun OkHttpClient.Builder.addDebugHttpLogging(): OkHttpClient.Builder {
    val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(HTTP_LOG_TAG, redactSensitiveContent(message))
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
        SENSITIVE_HEADERS.forEach(::redactHeader)
    }
    return addInterceptor(loggingInterceptor)
}

private const val HTTP_LOG_TAG = "OkHttp"

private val SENSITIVE_HEADERS = listOf(
    "Authorization",
    "Proxy-Authorization",
    "Cookie",
    "Set-Cookie",
    "X-Api-Key",
    "Api-Key",
    "api_key",
)

private val SENSITIVE_QUERY_PARAMETER = Regex(
    "(?i)((?:api_key|apikey|access_token|token|password|secret)=)([^&\\s]+)",
)

private fun redactSensitiveContent(message: String): String {
    return message.replace(SENSITIVE_QUERY_PARAMETER) { match ->
        "${match.groupValues[1]}██"
    }
}
