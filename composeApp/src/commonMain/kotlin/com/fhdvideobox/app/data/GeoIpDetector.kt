package com.fhdvideobox.app.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Определяет страну пользователя по внешнему IP.
 * Опрашивает сервисы по очереди — первый ответивший побеждает.
 * Свой эндпоинт (например https://api.ваш-домен/geo с полем country_code)
 * можно поставить первым в список.
 */
class GeoIpDetector(private val http: HttpClient) {

    /** URL -> имя поля с ISO-кодом страны */
    private val providers = listOf(
        "https://ipwho.is/" to "country_code",
        "https://ipapi.co/json/" to "country_code",
        "https://get.geojs.io/v1/ip/country.json" to "country",
        "https://api.country.is/" to "country",
    )

    /** "KZ", "RU", ... или null, если определить не удалось. */
    suspend fun detectCountry(): String? {
        for ((url, field) in providers) {
            val code = withTimeoutOrNull(6_000) {
                runCatching {
                    val text = http.get(url).bodyAsText()
                    (AppJson.parseToJsonElement(text) as? JsonObject)?.get(field)?.jsonPrimitive?.content
                }.getOrNull()
            }
            if (code != null && code.length == 2) return code.uppercase()
        }
        return null
    }
}
