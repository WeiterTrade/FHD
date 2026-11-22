package com.fhdvideobox.app.data

import com.fhdvideobox.app.AppConfig

/**
 * Регионы и их сервера. Чтобы добавить страну — добавьте элемент сюда
 * и адрес сервера в gradle.properties / секреты CI.
 */
enum class Region(
    val countryCode: String,   // ISO-код страны от гео-сервиса
    val title: String,
    val question: String,      // вопрос при первом входе
    val defaultUrl: String,
) {
    KZ("KZ", "Казахстан", "Вы находитесь в Казахстане?", AppConfig.SERVER_KZ),
    RU("RU", "Россия", "Вы находитесь в России?", AppConfig.SERVER_RU);

    companion object {
        fun fromCountry(code: String?): Region? =
            entries.firstOrNull { it.countryCode.equals(code, ignoreCase = true) }

        fun byName(name: String?): Region? = entries.firstOrNull { it.name == name }
    }
}
