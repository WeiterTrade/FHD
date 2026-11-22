package com.fhdvideobox.app.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val AppJson = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true
    install(ContentNegotiation) { json(AppJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 20_000
    }
    defaultRequest { headers.append("User-Agent", "FHDVideobox/${com.fhdvideobox.app.AppConfig.VERSION}") }
}

/**
 * Клиент API сервера региона. Контракт описан в docs/API.md.
 * baseUrl должен заканчиваться на "/".
 */
class VideoboxApi(private val http: HttpClient, baseUrl: String) {

    private val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    suspend fun catalog(section: Section, page: Int, sort: SortOrder): CatalogPage =
        http.get("${base}api/v1/catalog") {
            parameter("section", section.apiKey)
            parameter("page", page)
            parameter("sort", sort.apiKey)
        }.body()

    suspend fun details(id: String): ItemDetails =
        http.get("${base}api/v1/items/${id.encodeURLPathPart()}").body()

    suspend fun videos(id: String): List<VideoSource> =
        http.get("${base}api/v1/items/${id.encodeURLPathPart()}/videos").body()

    suspend fun search(query: String): CatalogPage =
        http.get("${base}api/v1/search") { parameter("q", query) }.body()

    /** Проверка доступности сервера. */
    suspend fun ping(): Boolean = runCatching { http.get("${base}api/v1/ping"); true }.getOrDefault(false)
}
