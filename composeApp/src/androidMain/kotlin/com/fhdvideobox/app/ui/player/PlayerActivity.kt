package com.fhdvideobox.app.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.widget.Toast

/**
 * Полноэкранный плеер (ExoPlayer). Работает и с сенсором, и с пультом:
 * ОК — пауза/панель, ←/→ — перемотка, «Назад» — выход.
 */
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var view: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        view = PlayerView(this).apply {
            keepScreenOn = true
            setShowSubtitleButton(true)
            controllerShowTimeoutMs = 4000
            requestFocus()
        }
        setContentView(view)

        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val sub = intent.getStringExtra(EXTRA_SUBTITLE)

        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().also { p ->
                view.player = p
                p.setMediaItem(
                    MediaItem.Builder().setUri(url)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setSubtitle(sub).build())
                        .build()
                )
                p.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(this@PlayerActivity, "Ошибка воспроизведения: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                    }
                })
                val pos = savedInstanceState?.getLong("pos") ?: 0L
                if (pos > 0) p.seekTo(pos)
                p.playWhenReady = true
                p.prepare()
            }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Перемотка стрелками пульта, когда панель управления скрыта
        val p = player
        if (p != null && event.action == KeyEvent.ACTION_DOWN && !view.isControllerFullyVisible) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { p.seekBack(); view.showController(); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { p.seekForward(); view.showController(); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    p.playWhenReady = !p.playWhenReady; view.showController(); return true
                }
            }
        }
        return view.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.let { outState.putLong("pos", it.currentPosition) }
    }

    override fun onStop() { super.onStop(); player?.pause() }

    override fun onDestroy() {
        player?.release(); player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SUBTITLE = "subtitle"

        fun intent(ctx: Context, title: String, subtitle: String?, url: String) =
            Intent(ctx, PlayerActivity::class.java)
                .putExtra(EXTRA_URL, url).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_SUBTITLE, subtitle)
    }
}
