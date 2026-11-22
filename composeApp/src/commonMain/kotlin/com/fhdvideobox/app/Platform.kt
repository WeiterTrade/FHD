package com.fhdvideobox.app

import androidx.compose.runtime.Composable
import com.fhdvideobox.app.data.KeyValueStore

/** То, что каждая платформа (Android / Windows / Linux) реализует сама. */
interface Platform {
    val name: String
    /** Android TV или ТВ-приставка: интерфейс под пульт. */
    val isTv: Boolean
    val storage: KeyValueStore
    fun nowMillis(): Long

    /** Запуск плеера с прямой ссылкой на поток (HLS/MP4). */
    fun play(title: String, subtitle: String?, url: String)

    /** Логотип (кубы) из ресурсов платформы. */
    @Composable
    fun logo(): androidx.compose.ui.graphics.painter.Painter

    fun openUrl(url: String)
    fun exit()
}

/** Обработчик кнопки «Назад» (пульт / системная кнопка / Esc). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
