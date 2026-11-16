package dev.alexrincon.movieclient.core.network

import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.addDebugHttpLogging(): OkHttpClient.Builder = this
