package com.fhdvideobox.app

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.fhdvideobox.app.ui.AppController
import com.fhdvideobox.app.ui.AppRoot
import com.fhdvideobox.app.player.DesktopPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.Desktop
import java.net.URI
import kotlin.system.exitProcess

fun main() {
    DesktopEnv.prepare()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    application {
        val state = rememberWindowState(size = DpSize(1280.dp, 760.dp), position = WindowPosition.PlatformDefault)
        var fullscreen by remember { mutableStateOf(false) }
        val platform = remember { DesktopPlatform { exitApplication() } }
        val app = remember { AppController(platform, scope) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "FHD Videobox",
            icon = painterResource("app_icon.png"),
            state = state,
            onPreviewKeyEvent = { e ->
                if (e.type != KeyEventType.KeyDown) false else when (e.key) {
                    // Esc / Backspace (вне текстовых полей) / кнопка «Назад» мыши — как «Назад» на пульте
                    Key.Escape -> BackDispatcher.dispatch()
                    Key.F11 -> {
                        fullscreen = !fullscreen
                        state.placement = if (fullscreen) androidx.compose.ui.window.WindowPlacement.Fullscreen
                        else androidx.compose.ui.window.WindowPlacement.Floating
                        true
                    }
                    else -> false
                }
            },
        ) {
            window.minimumSize = java.awt.Dimension(800, 520)
            AppRoot(app)
        }
    }
}

/** Эмуляция системной кнопки «Назад»: срабатывает последний зарегистрированный активный обработчик. */
object BackDispatcher {
    private class Entry(var enabled: Boolean, var onBack: () -> Unit)
    private val stack = mutableListOf<Entry>()

    fun dispatch(): Boolean {
        val e = stack.lastOrNull { it.enabled } ?: return false
        e.onBack(); return true
    }

    @Composable
    fun Register(enabled: Boolean, onBack: () -> Unit) {
        val entry = remember { Entry(enabled, onBack) }
        entry.enabled = enabled; entry.onBack = onBack
        DisposableEffect(entry) {
            stack.add(entry)
            onDispose { stack.remove(entry) }
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackDispatcher.Register(enabled, onBack)

class DesktopPlatform(private val onExit: () -> Unit) : Platform {
    override val name: String = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
    override val isTv: Boolean = false
    override val storage = FileKeyValueStore(DesktopEnv.dataDir.resolve("settings.json"))
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun play(title: String, subtitle: String?, url: String) = DesktopPlayer.open(title, subtitle, url)

    @Composable
    override fun logo(): Painter = painterResource("logo_cubes.png")

    override fun openUrl(url: String) {
        runCatching { Desktop.getDesktop().browse(URI(url)) }
    }

    override fun exit() { onExit(); exitProcess(0) }
}
