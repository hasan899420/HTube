package com.mdhasan.htube

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mdhasan.htube.core.common.Permissions
import com.mdhasan.htube.core.common.Urls
import com.mdhasan.htube.core.data.SettingsRepository
import com.mdhasan.htube.core.downloadsheet.DownloadSheetController
import com.mdhasan.htube.core.media.PlayerController
import com.mdhasan.htube.core.model.AppSettings
import com.mdhasan.htube.core.model.ThemeMode
import com.mdhasan.htube.core.navigation.DownloadingRoute
import com.mdhasan.htube.core.navigation.LibraryRoute
import com.mdhasan.htube.core.navigation.PlayerRoute
import com.mdhasan.htube.core.update.UpdateManager
import com.mdhasan.htube.core.update.UpdatePrompt
import com.mdhasan.htube.navigation.AppNavHost
import com.mdhasan.htube.navigation.UrlRouter
import com.mdhasan.htube.ui.AppTheme
import com.mdhasan.htube.ui.isDark
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settings: SettingsRepository by inject()
    private val router: UrlRouter by inject()
    private val sheet: DownloadSheetController by inject()
    private val player: PlayerController by inject()
    private val updates: UpdateManager by inject()
    private var pendingIntent by mutableStateOf<Intent?>(null)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestStartupPermissions()
        player.connect()
        // Look for a new GitHub release (throttled; release builds only unless the URL was overridden for testing).
        if (savedInstanceState == null) {
            updates.cleanUpInstalledUpdates()
            updates.consumeJustUpdated()?.let { v ->
                android.widget.Toast.makeText(this, "HTube updated to $v", android.widget.Toast.LENGTH_SHORT).show()
            }
            updates.checkOnLaunch(force = BuildConfig.UPDATE_URL_OVERRIDDEN)
        }
        if (savedInstanceState == null) pendingIntent = intent
        setContent {
            val s by settings.settings.collectAsStateWithLifecycle(AppSettings())
            val nav = rememberNavController()
            val dark = s.isDark()
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            AppTheme(s) {
                AppNavHost(nav)
                UpdatePrompt()
            }
            LaunchedEffect(pendingIntent) {
                val i = pendingIntent ?: return@LaunchedEffect
                pendingIntent = null
                handleIntent(i) { route -> nav.navigate(route) { launchSingleTop = true } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntent = intent
    }

    private fun handleIntent(intent: Intent, navigate: (Any) -> Unit) {
        intent.getStringExtra("open")?.let { open ->
            when {
                open == "downloads" -> navigate(DownloadingRoute)
                open == "player" -> navigate(PlayerRoute)
                open.startsWith("play:") -> navigate(LibraryRoute)
            }
            return
        }
        val url = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> Urls.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
            else -> null
        } ?: return
        // Shared text or the share popup's fallback means "download this"; a tapped YouTube link just opens it.
        val pasted = intent.action == Intent.ACTION_SEND || intent.getBooleanExtra(EXTRA_SHOW_SHEET, false)
        navigate(if (pasted) router.routeForPasted(url) else router.routeFor(url))
    }

    private fun requestStartupPermissions() {
        // Only what this Android version needs: notifications (13+) and storage (10 and older).
        val perms = (Permissions.notifications + Permissions.storageWrite).filterNot { Permissions.granted(this, arrayOf(it)) }
        if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
    }

    companion object {
        const val EXTRA_SHOW_SHEET = "show_sheet"
    }
}
