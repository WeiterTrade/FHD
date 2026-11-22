package com.fhdvideobox.app.player

import com.fhdvideobox.app.DesktopEnv
import com.sun.jna.NativeLibrary
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.fullscreen.adaptive.AdaptiveFullScreenStrategy
import java.awt.*
import java.awt.event.*
import java.io.File
import javax.swing.*

/**
 * Плеер для Windows/Linux на libVLC (vlcj).
 * Windows: libVLC лежит рядом с программой (resources/vlc), ставить VLC не нужно.
 * Linux: используется системный VLC (пакет vlc / libvlc5).
 */
object DesktopPlayer {

    private var frame: PlayerFrame? = null
    private val vlcReady: Boolean by lazy { VlcLoader.load() }

    fun open(title: String, subtitle: String?, url: String) {
        SwingUtilities.invokeLater {
            if (!vlcReady) { showNoVlc(url); return@invokeLater }
            frame?.close()
            frame = runCatching { PlayerFrame(title, subtitle, url) }
                .onFailure { showNoVlc(url) }.getOrNull()
        }
    }

    private fun showNoVlc(url: String) {
        val hint = if (DesktopEnv.isWindows) "Переустановите приложение — компонент видео повреждён."
        else "Установите VLC:  sudo apt install vlc   (или dnf install vlc)"
        val choice = JOptionPane.showOptionDialog(
            null, "Не найден видеодвижок VLC.\n$hint", "FHD Videobox",
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
            arrayOf("Открыть во внешнем плеере", "Закрыть"), "Закрыть",
        )
        if (choice == 0) runCatching { Desktop.getDesktop().browse(java.net.URI(url)) }
    }
}

private object VlcLoader {
    fun load(): Boolean {
        val bundled: File? = DesktopEnv.resourcesDir?.resolve("vlc")
        if (bundled != null && bundled.isDirectory) {
            val (vlc, core) = if (DesktopEnv.isWindows) "libvlc" to "libvlccore" else "vlc" to "vlccore"
            NativeLibrary.addSearchPath(core, bundled.absolutePath)
            NativeLibrary.addSearchPath(vlc, bundled.absolutePath)
        }
        return runCatching {
            if (bundled == null || !bundled.isDirectory) uk.co.caprica.vlcj.factory.discovery.NativeDiscovery().discover()
            // Пробная загрузка: если библиотеки нет — будет исключение
            uk.co.caprica.vlcj.factory.MediaPlayerFactory().release()
            true
        }.getOrDefault(false)
    }
}

private class PlayerFrame(title: String, subtitle: String?, url: String) : JFrame("$title — FHD Videobox") {

    private val component = EmbeddedMediaPlayerComponent()
    private val player get() = component.mediaPlayer()
    private val playBtn = JButton("❚❚")
    private val slider = JSlider(0, 1000, 0)
    private val time = JLabel("00:00 / 00:00")
    private val bar = JPanel(BorderLayout(8, 0))
    private var dragging = false
    private var fullscreen = false

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        iconImage = runCatching { javax.imageio.ImageIO.read(javaClass.getResource("/app_icon.png")) }.getOrNull()
        background = Color.BLACK

        val dark = Color(20, 20, 20); val fg = Color(230, 230, 230)
        listOf(playBtn).forEach { it.isFocusable = false; it.foreground = fg; it.background = dark; it.isBorderPainted = false }
        slider.isFocusable = false; slider.background = dark
        time.foreground = fg
        val info = JLabel(listOfNotNull(title, subtitle).joinToString("   •   ")).apply { foreground = Color(160, 160, 160) }
        bar.background = dark
        bar.border = BorderFactory.createEmptyBorder(6, 10, 6, 10)
        bar.add(playBtn, BorderLayout.WEST)
        bar.add(slider, BorderLayout.CENTER)
        bar.add(JPanel(BorderLayout()).apply { isOpaque = false; add(time, BorderLayout.EAST); add(info, BorderLayout.NORTH) }, BorderLayout.EAST)

        contentPane.layout = BorderLayout()
        contentPane.add(component, BorderLayout.CENTER)
        contentPane.add(bar, BorderLayout.SOUTH)
        setSize(1100, 660)
        setLocationRelativeTo(null)

        playBtn.addActionListener { togglePause() }
        slider.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { dragging = true }
            override fun mouseReleased(e: MouseEvent) {
                dragging = false
                player.controls().setPosition(slider.value / 1000f)
            }
        })

        component.videoSurfaceComponent().addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) { if (e.clickCount == 2) toggleFullscreen() else togglePause() }
        })

        // Горячие клавиши: пробел — пауза, ←/→ — 10 сек, ↑/↓ — громкость, F/F11 — полный экран, Esc — выход
        val keys = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> togglePause()
                    KeyEvent.VK_LEFT -> player.controls().skipTime(-10_000)
                    KeyEvent.VK_RIGHT -> player.controls().skipTime(10_000)
                    KeyEvent.VK_UP -> player.audio().setVolume(minOf(200, player.audio().volume() + 10))
                    KeyEvent.VK_DOWN -> player.audio().setVolume(maxOf(0, player.audio().volume() - 10))
                    KeyEvent.VK_F, KeyEvent.VK_F11 -> toggleFullscreen()
                    KeyEvent.VK_ESCAPE -> if (fullscreen) toggleFullscreen() else close()
                }
            }
        }
        addKeyListener(keys)
        component.videoSurfaceComponent().addKeyListener(keys)

        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun positionChanged(mediaPlayer: MediaPlayer, newPosition: Float) {
                SwingUtilities.invokeLater { if (!dragging) slider.value = (newPosition * 1000).toInt(); updateTime() }
            }
            override fun playing(mediaPlayer: MediaPlayer) { SwingUtilities.invokeLater { playBtn.text = "❚❚" } }
            override fun paused(mediaPlayer: MediaPlayer) { SwingUtilities.invokeLater { playBtn.text = "▶" } }
            override fun error(mediaPlayer: MediaPlayer) {
                SwingUtilities.invokeLater { JOptionPane.showMessageDialog(this@PlayerFrame, "Не удалось воспроизвести видео.", "Ошибка", JOptionPane.ERROR_MESSAGE) }
            }
            override fun finished(mediaPlayer: MediaPlayer) { SwingUtilities.invokeLater { playBtn.text = "▶" } }
        })

        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) { component.release() }
        })

        // Полный экран без пересоздания окна (на Windows — через Win32 API, на Linux — X11)
        player.fullScreen().strategy(object : AdaptiveFullScreenStrategy(this) {
            override fun onBeforeEnterFullScreen() { bar.isVisible = false }
            override fun onAfterExitFullScreen() { bar.isVisible = true }
        })

        isVisible = true
        requestFocus()
        player.media().play(url, ":network-caching=1500")
    }

    private fun togglePause() = player.controls().pause()

    private fun updateTime() {
        fun fmt(ms: Long): String {
            val s = (ms / 1000).coerceAtLeast(0); val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
        }
        time.text = "${fmt(player.status().time())} / ${fmt(player.status().length())}"
    }

    private fun toggleFullscreen() {
        player.fullScreen().toggle()
        fullscreen = player.fullScreen().isFullScreen
        requestFocus()
    }

    fun close() {
        if (player.fullScreen().isFullScreen) player.fullScreen().set(false)
        dispose()
    }
}
