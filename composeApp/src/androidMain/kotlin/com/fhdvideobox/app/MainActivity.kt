package com.fhdvideobox.app

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.fhdvideobox.app.data.KeyValueStore
import com.fhdvideobox.app.ui.AppController
import com.fhdvideobox.app.ui.AppRoot
import com.fhdvideobox.app.ui.player.PlayerActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = AppController(AndroidPlatform(this), lifecycleScope)
        setContent { AppRoot(app) }
    }
}

class SharedPrefsStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("fhd_settings", Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

class AndroidPlatform(private val activity: ComponentActivity) : Platform {

    override val isTv: Boolean = run {
        val ui = activity.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        ui.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            !activity.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }

    override val name: String = if (isTv) "Android TV" else "Android"
    override val storage: KeyValueStore = SharedPrefsStore(activity)
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun play(title: String, subtitle: String?, url: String) {
        activity.startActivity(PlayerActivity.intent(activity, title, subtitle, url))
    }

    @Composable
    override fun logo(): Painter = painterResource(R.drawable.logo_cubes)

    override fun openUrl(url: String) {
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    override fun exit() { activity.finish() }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
