package com.fhdvideobox.app

import com.fhdvideobox.app.data.AppJson
import com.fhdvideobox.app.data.KeyValueStore
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

object DesktopEnv {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    /** Windows 7 = 6.1, Windows 8 = 6.2, 8.1 = 6.3 */
    val isLegacyWindows = isWindows && (System.getProperty("os.version").toDoubleOrNull() ?: 10.0) < 6.3

    val dataDir: File by lazy {
        val dir = if (isWindows) File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "FHDVideobox")
        else File(System.getenv("XDG_CONFIG_HOME") ?: (System.getProperty("user.home") + "/.config"), "fhdvideobox")
        dir.apply { mkdirs() }
    }

    /** Папка ресурсов, упакованных рядом с приложением (libVLC на Windows). */
    val resourcesDir: File? = System.getProperty("compose.application.resources.dir")?.let(::File)

    fun prepare() {
        // Windows 7/8: нет DirectX 12 — сразу рисуем через OpenGL, при сбое Skiko сам уйдёт в программный режим
        if (isLegacyWindows && System.getProperty("skiko.renderApi") == null) {
            System.setProperty("skiko.renderApi", "OPENGL")
        }
        // Чёткий текст на HiDPI в Linux
        System.setProperty("sun.java2d.uiScale.enabled", "true")
    }
}

/** Настройки ПК: JSON-файл в %APPDATA%\FHDVideobox или ~/.config/fhdvideobox */
class FileKeyValueStore(private val file: File) : KeyValueStore {
    private val ser = MapSerializer(String.serializer(), String.serializer())
    private val map: MutableMap<String, String> =
        runCatching { AppJson.decodeFromString(ser, file.readText()).toMutableMap() }.getOrElse { mutableMapOf() }

    @Synchronized override fun get(key: String): String? = map[key]

    @Synchronized override fun put(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(AppJson.encodeToString(ser, map))
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
    }
}
