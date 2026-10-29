package com.mdhasan.htube.share

import androidx.compose.runtime.key
import com.mdhasan.htube.feature.browser.BrowserScreen
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdhasan.htube.MainActivity
import com.mdhasan.htube.core.common.Urls
import com.mdhasan.htube.core.data.SettingsRepository
import com.mdhasan.htube.ui.AppTheme
import com.mdhasan.htube.core.downloadsheet.DownloadSheetController
import com.mdhasan.htube.core.downloadsheet.DownloadSheetHost
import com.mdhasan.htube.core.model.AppSettings
import com.mdhasan.htube.core.model.ThemeMode
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

/**
 * Target of "Share → HTube" from YouTube, Facebook, TikTok, etc.
 * Shows only the format picker over the current app; the download continues in the background.
 */
class ShareActivity : ComponentActivity() {
    private val sheet: DownloadSheetController by inject()
    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!openFrom(intent)) return
        setContent {
            val s by settings.settings.collectAsStateWithLifecycle(AppSettings())
            val target by sheet.target.collectAsStateWithLifecycle()
            AppTheme(s) {
                deep?.let { (url, run) ->
                    key(run) {
                        Box(Modifier.fillMaxSize().alpha(0f)) {
                            BrowserScreen(
                                startUrl = url, autoDownload = true, silent = true, stateKey = "share-$run",
                                onClose = {}, onOpenVideo = {}, onOpenHistory = {}, onOpenBookmarks = {},
                            )
                        }
                        // The page is invisible: never let a tap reach it.
                        Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } })
                    }
                }
                DownloadSheetHost(onOpenInBrowser = ::openInApp, onDeepSearch = { deep = it to ++deepRuns }, onOpenExisting = ::openDownload)
            }
            // Sheet closed (downloaded or dismissed) → return to the app the user came from.
            LaunchedEffect(target) { if (target == null) finish() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFrom(intent)
    }

    /**
     * Same detection as a pasted link (which opens in the HTube browser), but invisible and muted behind
     * the sheet, so sharing finds every video pasting would, without leaving the other app.
     */
    private var deep by mutableStateOf<Pair<String, Int>?>(null)
    private var deepRuns = 0

    private fun openFrom(intent: Intent): Boolean {
        // A new share replaces the last one entirely: its invisible page must not reopen the sheet later.
        deep = null
        val url = Urls.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
        if (url == null) {
            Toast.makeText(this, "No link found to download", Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        sheet.open(url)
        get<com.mdhasan.htube.core.analytics.Analytics>().event(com.mdhasan.htube.core.analytics.Events.SHARE_TO_DOWNLOAD)
        return true
    }

    /** "Open" in the "already downloaded" dialog: HTube's Play tab (or Downloading, if still in progress). */
    private fun openDownload(task: com.mdhasan.htube.core.model.DownloadTask) {
        val done = task.status == com.mdhasan.htube.core.model.DownloadStatus.COMPLETED
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("open", if (done) "play:${task.id}" else "downloads"),
        )
        finish()
    }

    fun openInApp(url: String) {
        startActivity(
            Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(android.net.Uri.parse(url))
                .putExtra(MainActivity.EXTRA_SHOW_SHEET, true),
        )
        finish()
    }
}
