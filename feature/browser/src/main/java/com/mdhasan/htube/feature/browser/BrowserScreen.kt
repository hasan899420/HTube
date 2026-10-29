package com.mdhasan.htube.feature.browser

import com.mdhasan.htube.core.designsystem.QtIconButton
import com.mdhasan.htube.core.designsystem.icon.QtIcons
import java.io.ByteArrayInputStream
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import com.mdhasan.htube.core.common.Format
import com.mdhasan.htube.core.designsystem.PillButton
import androidx.compose.foundation.background
import com.mdhasan.htube.core.designsystem.pressable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdhasan.htube.core.model.AppSettings
import org.koin.compose.koinInject
import com.mdhasan.htube.core.common.Urls
import com.mdhasan.htube.core.playerui.findActivity
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private val YT_WATCH = Regex("""youtube\.com/(watch\?|shorts/)|youtu\.be/""")

/**
 * Reports every <video> on the page (playing state, size, duration, sources) plus the page title/image.
 * Also hooks play events so a video is reported the moment it starts.
 */
private const val SNIFF_JS = """
(function(){try{
 if(!window.__qtHooked){window.__qtHooked=true; window.__qtId=Math.random().toString(36).slice(2); window.__qtTouch=0;
  ['touchstart','pointerdown','click','keydown'].forEach(function(e){document.addEventListener(e,function(ev){window.__qtTouch=Date.now();
   // A tap on a video (or its controls overlay) means the user picked that video.
   var t=ev.touches&&ev.touches[0]?ev.touches[0]:ev; if(t.clientX==null||!window.__qtSeen)return;
   window.__qtSeen.forEach(function(v){var r=v.getBoundingClientRect();
    if(t.clientX>=r.left&&t.clientX<=r.right&&t.clientY>=r.top&&t.clientY<=r.bottom)v.__qtUser=true;});},true);});
  window.__qtSeen=new Set();
  function mark(v){try{window.__qtSeen.add(v);var ua=navigator.userActivation;
   if((ua&&ua.isActive)||Date.now()-window.__qtTouch<5000)v.__qtUser=true;}catch(x){}}
  document.addEventListener('play',function(e){mark(e.target);},true);
  // Players inside shadow DOM don't send events to the document; catch them when they call play().
  try{var op=HTMLMediaElement.prototype.play;HTMLMediaElement.prototype.play=function(){mark(this);setTimeout(function(){window.__qtScan&&window.__qtScan();},300);return op.apply(this,arguments);};}catch(x){}
  ['play','playing','loadedmetadata'].forEach(function(e){document.addEventListener(e,function(){setTimeout(function(){window.__qtScan&&window.__qtScan();},250);},true);});
  setInterval(function(){window.__qtScan&&window.__qtScan();},2000);}
 window.__qtScan=function(){try{
  var vw=window.innerWidth||document.documentElement.clientWidth||0, vids=[], all=new Set();
  function walk(root){root.querySelectorAll('video').forEach(function(v){all.add(v);});
   root.querySelectorAll('*').forEach(function(el){if(el.shadowRoot)walk(el.shadowRoot);});}
  walk(document);
  if(window.__qtSeen)window.__qtSeen.forEach(function(v){if(v.isConnected&&v.tagName==='VIDEO')all.add(v);});
  all.forEach(function(v){ if(window.__qtSeen)window.__qtSeen.add(v);
   var r=v.getBoundingClientRect(), s=[];
   if(v.currentSrc)s.push(v.currentSrc); if(v.src)s.push(v.src);
   v.querySelectorAll('source').forEach(function(x){if(x.src)s.push(x.src);});
   vids.push({s:s,u:!!v.__qtUser,p:!v.paused&&!v.ended,t:v.currentTime||0,d:isFinite(v.duration)?v.duration:-1,m:!!v.muted,l:!!v.loop,w:r.width||0,vw:vw,po:v.poster||''});
  });
  function meta(p){var m=document.querySelector('meta[property="'+p+'"],meta[name="'+p+'"]');return m?m.content:null;}
  if(!vids.length&&window!==window.top)return;
  QtSniffer.report(JSON.stringify({f:window.__qtId,top:window===window.top,v:vids,t:meta('og:title')||document.title,i:meta('og:image')}));
 }catch(e){}};
 window.__qtScan();
}catch(e){}})();
"""

/**
 * For the invisible browser behind the share sheet: every video plays at volume 0 (nothing is heard) and counts
 * as started by the user, so the page's main video is detected exactly as when the user taps play.
 */
private const val SILENT_JS = """
(function(){try{
 if(window.__qtSilent)return; window.__qtSilent=true;
 function hush(v){try{v.__qtUser=true; if(v.volume!==0)v.volume=0;}catch(x){}}
 ['play','playing','volumechange','loadedmetadata'].forEach(function(e){document.addEventListener(e,function(ev){
  if(ev.target&&ev.target.tagName&&/^(VIDEO|AUDIO)$/.test(ev.target.tagName))hush(ev.target);},true);});
 try{var op=HTMLMediaElement.prototype.play;HTMLMediaElement.prototype.play=function(){hush(this);return op.apply(this,arguments);};}catch(x){}
 document.querySelectorAll('video,audio').forEach(hush);
 // Nobody can tap play on an invisible page, so start the player like the user would (silently). Many players
 // (e.g. Vimeo's) only load the video once play is pressed.
 function kick(){try{
  var vs=document.querySelectorAll('video'), playing=false;
  vs.forEach(function(v){if(!v.paused&&!v.ended)playing=true;});
  if(playing)return;
  vs.forEach(function(v){hush(v);var p=v.play();if(p&&p.catch)p.catch(function(){});});
  var b=document.querySelector('[aria-label="Play"],[aria-label^="Play video"],[data-play-button],.vp-big-play-button,.vjs-big-play-button,.ytp-large-play-button,.jw-display-icon-display,.plyr__control--overlaid');
  if(b)b.click();
 }catch(x){}}
 setTimeout(kick,800); setInterval(kick,2500);
}catch(e){}})();
"""

/**
 * Ads Blocker (Settings → Ads Blocker, on by default). Network level: any
 * non-main-frame request to a known ad/tracker host returns an empty
 * response, so the ad script/image never loads. Page level: AD_HIDE_JS
 * removes ad iframes/banners that slipped through. This is not 100%:
 * first-party ads served from the site's own host cannot be blocked by
 * domain without breaking the video, and new ad domains appear daily.
 * Video requests (.mp4/.m3u8) are checked before this, so downloads
 * are never blocked as ads.
 */
private val AD_HOSTS = listOf(
    // Combined list: EasyList + EasyPrivacy + AdGuard Mobile + Peter Lowe's
    // ad-server list + adult ad networks. Domain-only blocking (no full
    // EasyList regex engine in WebView), so this is a curated host list,
    // not a verbatim copy of any one list. Video CDNs and site hosts are
    // deliberately NOT in here, so downloads are never blocked as ads.
    // Google / general ads
    "doubleclick.net", "googlesyndication.com", "googleads", "googleadservices.com",
    "adsafeprotected.com", "adsense.com", "adservice.google.com", "pagead2.googlesyndication.com",
    "advertising.com", "ads.yahoo.com", "amazon-adsystem.com", "adsrvr.org",
    "adform.net", "smartadserver.com", "adnxs.com", "criteo.com", "rubiconproject.com",
    "pubmatic.com", "openx.net", "moatads.com", "scorecardresearch.com", "quantserve.com",
    "hotjar.com", "inmobi.com", "adcolony.com", "appnexus.com", "bidswitch.net",
    "casalemedia.com", "districtm.io", "sharethrough.com", "triplelift.com",
    "teads.tv", "outbrain.com", "taboola.com", "mgid.com", "revcontent.com",
    "zedo.com", "bidvertiser.com", "chocolateplatform.com", "lijit.com",
    "sonobi.com", "sovrn.com", "yieldmo.com", "rhythmone.com", "conversantmedia.com",
    // Pop / push / redirect ads
    "popads.net", "popcash.net", "adcash.com", "propellerads.com", "adsterra.com",
    "clickadu.com", "hilltopads.com", "richads.com", "pushwoosh.com", "onesignal.com",
    "richpush.co", "pushengage.com", "subscribers.com", "ad-maven.com", "popmyads.com",
    "onclickads.net", "propellerads.com", "adcash.com", "trafficstars.com",
    // Adult ad networks (the sites MD added)
    "exoclick.com", "trafficjunky.com", "juicyads.com", "plugrush.com", "adxpansion.com",
    "ero-advertising.com", "trafficforce.com", "adnium.com", "xxxreactor.com",
    "tubeadvertising.eu", "ads.trafficjunky.net", "cdn1.exoclick.com",
    // Trackers / analytics that also serve ads
    "fullstory.com", "mouseflow.com", "crazyegg.com", "luckyorange.com",
    "matomo.cloud", "stats.wp.com", "pixel.wp.com",
)

private fun isAdUrl(url: String): Boolean {
    val lower = url.lowercase()
    if (lower.contains("/ads/") || lower.contains("adserver") || lower.contains("popunder")) return true
    val host = runCatching { android.net.Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
    return AD_HOSTS.any { host == it || host.endsWith(".$it") }
}

private const val AD_HIDE_JS = """
(function(){try{
 function hide(){try{
  document.querySelectorAll('iframe').forEach(function(f){var s=(f.src||'').toLowerCase();
   if(s.indexOf('doubleclick')>=0||s.indexOf('googlesyndication')>=0||s.indexOf('exoclick')>=0||s.indexOf('trafficjunky')>=0||s.indexOf('taboola')>=0||s.indexOf('outbrain')>=0||s.indexOf('mgid')>=0||s.indexOf('adsterra')>=0||s.indexOf('propellerads')>=0||s.indexOf('/ads/')>=0)f.remove();});
  document.querySelectorAll('[id^="ad_"],[id^="ads_"],[id^="advert"],[class*="advertisement"],[class*="ad-banner"]').forEach(function(e){e.remove();});
 }catch(x){}}
 hide(); setTimeout(hide,1500);
}catch(e){}})();
"""

/** Hands the rendered page (after scripts, with the user's session) to the page parser. */
private fun grabHtml(view: WebView, vm: BrowserViewModel) {
    val url = view.url ?: return
    view.evaluateJavascript("document.documentElement.outerHTML") { quoted ->
        val html = runCatching { org.json.JSONArray("[$quoted]").getString(0) }.getOrNull() ?: return@evaluateJavascript
        vm.onPageHtml(url, html)
    }
}

/**
 * Runs fetch() inside the page so requests carry the page's own cookies, origin and TLS session —
 * used for quality lists and playlists that refuse requests from outside the browser.
 */
private class PageFetcher {
    private val pending = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.CompletableDeferred<String?>>()
    var webView: WebView? = null

    @JavascriptInterface
    fun fetchResult(id: String, text: String?) {
        pending.remove(id)?.complete(text)
    }

    suspend fun fetch(url: String): String? {
        val view = webView ?: return null
        val id = java.util.UUID.randomUUID().toString()
        val d = kotlinx.coroutines.CompletableDeferred<String?>()
        pending[id] = d
        val js = "fetch(" + org.json.JSONObject.quote(url) + ",{credentials:'include'}).then(function(r){return r.ok?r.text():null})" +
            ".then(function(t){QtFetch.fetchResult('" + id + "',t)}).catch(function(){QtFetch.fetchResult('" + id + "',null)});"
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { view.evaluateJavascript(js, null) }
        return kotlinx.coroutines.withTimeoutOrNull(15_000) { d.await() }.also { pending.remove(id) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    startUrl: String,
    autoDownload: Boolean = false,
    onClose: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    /** Invisible and muted (behind the share sheet): no sound, no history. */
    silent: Boolean = false,
    /** Separates browser states that open the same URL (each share gets a fresh one). */
    stateKey: String = startUrl,
) {
    val vm = koinViewModel<BrowserViewModel>(key = stateKey)
    val fetcher = remember { PageFetcher() }
    LaunchedEffect(Unit) { vm.autoDownload = autoDownload; vm.pageFetch = fetcher::fetch; vm.recordHistory = !silent }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val saved by vm.bookmarked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Ads Blocker toggle from Settings; read live so toggling it and
    // reloading a page takes effect without restarting the browser.
    val settingsRepo = koinInject<com.mdhasan.htube.core.data.SettingsRepository>()
    val appSettings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val adsBlockerOn by androidx.compose.runtime.rememberUpdatedState(appSettings.adsBlockerEnabled)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var editing by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    val openVideo by rememberUpdatedState(onOpenVideo)

    val isNative = YT_WATCH.containsMatchIn(ui.url)
    // Only when the user is on a video and it is playing — never on listing/home pages.
    val canDownload = isNative || ui.mainVideo != null || ui.pageMedia != null

    BackHandler {
        when {
            customView != null -> webView?.webChromeClient?.onHideCustomView()
            editing -> editing = false
            webView?.canGoBack() == true -> webView?.goBack()
            else -> onClose()
        }
    }

    // Periodically re-scan the page for players that load lazily.
    LaunchedEffect(webView, ui.url) {
        while (true) {
            delay(2500)
            if (silent) webView?.evaluateJavascript(SILENT_JS, null)
            webView?.evaluateJavascript(SNIFF_JS, null)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                QtIconButton(QtIcons.Close, "Close", onClose)
                if (editing) {
                    TextField(
                        address, { address = it }, Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            val t = address.trim()
                            webView?.loadUrl(if (Urls.looksLikeUrl(t)) Urls.normalize(t) else Urls.googleSearch(t))
                            editing = false
                        }),
                    )
                } else {
                    Row(
                        Modifier.weight(1f).padding(vertical = 8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .pressable(scaleDown = 0.98f, onClick = { address = ui.url; editing = true }).padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (ui.url.startsWith("https")) QtIcons.Lock else QtIcons.Public, null, Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(ui.title.ifBlank { Urls.host(ui.url).orEmpty() }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge)
                            Text(Urls.host(ui.url).orEmpty(), maxLines = 1, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                QtIconButton(QtIcons.Refresh, "Reload", { webView?.reload() })
                Box {
                    QtIconButton(QtIcons.MoreVert, "More", { menu = true })
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(if (saved) "Saved" else "Save page") }, leadingIcon = {
                            Icon(if (saved) QtIcons.Bookmark else QtIcons.BookmarkBorder, null)
                        }, onClick = { vm.toggleBookmark(); menu = false })
                        DropdownMenuItem(text = { Text("Saved pages") }, leadingIcon = { Icon(QtIcons.Collections, null) },
                            onClick = { menu = false; onOpenBookmarks() })
                        DropdownMenuItem(text = { Text("History") }, leadingIcon = { Icon(QtIcons.History, null) },
                            onClick = { menu = false; onOpenHistory() })
                        DropdownMenuItem(text = { Text("Copy link") }, leadingIcon = { Icon(QtIcons.ContentCopy, null) }, onClick = {
                            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("url", ui.url))
                            menu = false
                        })
                        DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(QtIcons.Share, null) }, onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ui.url), "Share"))
                            menu = false
                        })
                        DropdownMenuItem(text = { Text("Open in browser") }, leadingIcon = { Icon(QtIcons.OpenInNew, null) }, onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ui.url))) }
                            menu = false
                        })
                    }
                }
            }
            if (ui.progress in 1..99) LinearProgressIndicator(progress = { ui.progress / 100f }, modifier = Modifier.fillMaxWidth())
            ui.loadError?.let { err ->
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Page couldn't load", style = MaterialTheme.typography.labelLarge)
                    Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Text("If this site blocks the in-app browser, try it in your external browser.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 8.dp)) {
                        Button(onClick = { vm.clearLoadError(); webView?.reload() }) { Text("Retry") }
                        TextButton(onClick = {
                            vm.clearLoadError()
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ui.url))) }
                        }) { Text("Open in browser") }
                    }
                }
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.allowContentAccess = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.textZoom = 100
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        // Ad scripts open pop-ups/pop-unders on any tap; only real links the user tapped may open.
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        // Run the detector in every frame (embedded players live in iframes) from the very start.
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                            WebViewCompat.addDocumentStartJavaScript(this, SNIFF_JS, setOf("*"))
                            if (silent) WebViewCompat.addDocumentStartJavaScript(this, SILENT_JS, setOf("*"))
                        }
                        // Sites that detect WebView (the "wv" marker, or a WebView
                        // Chrome version) block it and show a blank page, while the
                        // same site works in Chrome. Use a plain Chrome Android UA
                        // so the in-app WebView looks like ordinary Chrome.
                        val cleanUa = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                        settings.userAgentString = cleanUa
                        vm.userAgent = cleanUa
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun report(json: String) {
                                val o = runCatching { org.json.JSONObject(json) }.getOrNull() ?: return
                                val arr = o.optJSONArray("v") ?: org.json.JSONArray()
                                val videos = (0 until arr.length()).map { i ->
                                    val v = arr.getJSONObject(i)
                                    val srcs = v.optJSONArray("s")
                                    PageVideo(
                                        sources = (0 until (srcs?.length() ?: 0)).map { srcs!!.getString(it) }.distinct(),
                                        userStarted = v.optBoolean("u"),
                                        playing = v.optBoolean("p"), currentTime = v.optDouble("t", 0.0), duration = v.optDouble("d", -1.0),
                                        muted = v.optBoolean("m"), loop = v.optBoolean("l"), width = v.optDouble("w", 0.0),
                                        viewportWidth = v.optDouble("vw", 0.0), poster = v.optString("po").ifBlank { null },
                                    )
                                }
                                val title = o.optString("t").ifBlank { null }
                                val image = if (o.isNull("i")) null else o.optString("i").ifBlank { null }
                                val frame = o.optString("f")
                                val top = o.optBoolean("top", true)
                                post { vm.onPageReport(frame, top, videos, if (top) title else null, if (top) image else null) }
                            }
                        }, "QtSniffer")
                        addJavascriptInterface(fetcher, "QtFetch")
                        fetcher.webView = this
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val u = request.url.toString()
                                if (u.startsWith("http")) return false
                                // The invisible page behind the share sheet must never open another app (e.g. Facebook's
                                // "open in app" bounce); it stays on the web page.
                                if (silent) {
                                    runCatching { Intent.parseUri(u, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url") }
                                        .getOrNull()?.takeIf { it.startsWith("http") }?.let(view::loadUrl)
                                    return true
                                }
                                val intent = runCatching {
                                    if (u.startsWith("intent:")) Intent.parseUri(u, Intent.URI_INTENT_SCHEME) else Intent(Intent.ACTION_VIEW, Uri.parse(u))
                                }.getOrNull() ?: return true
                                // Sites try to bounce visitors into their app; stay in the browser unless the user tapped a link.
                                val fallback = intent.getStringExtra("browser_fallback_url")
                                if (!request.hasGesture()) {
                                    if (fallback != null) view.loadUrl(fallback)
                                    return true
                                }
                                runCatching { view.context.startActivity(intent) }.onFailure { if (fallback != null) view.loadUrl(fallback) }
                                return true
                            }

                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = vm.onPageStarted(url)

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) vm.onLoadError(error.description?.toString() ?: "Failed to load page")
                            }

                            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                                if (request.isForMainFrame && errorResponse.statusCode >= 400) vm.onLoadError("HTTP ${errorResponse.statusCode}")
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                vm.onPageFinished(url, view.title)
                                if (silent) view.evaluateJavascript(SILENT_JS, null)
                                if (adsBlockerOn) view.evaluateJavascript(AD_HIDE_JS, null)
                                view.evaluateJavascript(SNIFF_JS, null)
                                grabHtml(view, vm)
                                // Players often inject their config a moment after load.
                                view.postDelayed({ if (view.url == url) grabHtml(view, vm) }, 2500)
                            }

                            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                                vm.onUrlChanged(url, view.title)
                                view.postDelayed({ if (view.url == url) grabHtml(view, vm) }, 1500)
                                // Same as the original app: tapping a YouTube video opens the native video page.
                                if (YT_WATCH.containsMatchIn(url) && !url.contains("/shorts/")) {
                                    view.post {
                                        openVideo(url)
                                        if (view.canGoBack()) view.goBack()
                                    }
                                }
                            }

                            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                val u = request.url.toString()
                                val path = request.url.path.orEmpty().lowercase()
                                // Video first: never let the ad filter eat a download.
                                if (path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".m4a") || path.endsWith(".mp3") ||
                                    path.endsWith(".webm") || path.endsWith(".mov") || path.endsWith(".flv") || path.endsWith(".m3u8") ||
                                    u.contains("mime_type=video_mp4") || u.contains(".mp4?") || u.contains(".m3u8?")
                                ) view.post { vm.onMediaRequest(u) }
                                else if (adsBlockerOn && !request.isForMainFrame && isAdUrl(u)) {
                                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                                }
                                return null
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) = vm.onProgress(newProgress, view.canGoBack())

                            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                                // target=_blank links the user tapped open here; script pop-ups are dropped.
                                val hit = view.hitTestResult
                                val link = if (isUserGesture && (hit.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                                        hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) hit.extra else null
                                if (link != null && link.startsWith("http")) view.loadUrl(link)
                                return false
                            }
                            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                val decor = ctx.findActivity()?.window?.decorView as? FrameLayout ?: return
                                decor.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                                customView = view
                            }
                            override fun onHideCustomView() {
                                val decor = ctx.findActivity()?.window?.decorView as? FrameLayout ?: return
                                customView?.let { decor.removeView(it) }
                                customView = null
                            }
                        }
                        // The invisible page behind the share sheet never offers files (an ad could trigger one).
                        if (!silent) setDownloadListener { url, ua, disposition, mime, _ ->
                            vm.downloadFile(url, mime, URLUtil.guessFileName(url, disposition, mime))
                        }
                        if (silent && WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) WebViewCompat.setAudioMuted(this, true)
                        loadUrl(startUrl)
                        webView = this
                    }
                },
            )
            // Own bar under the page (never over it), so page elements and ad overlays can't steal the tap.
            AnimatedVisibility(canDownload, enter = expandVertically(), exit = shrinkVertically()) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val d = ui.pageMedia?.durationSec ?: ui.mainVideo?.duration?.takeIf { it > 0 }
                    Column(Modifier.weight(1f)) {
                        Text("Video found on this page", style = MaterialTheme.typography.labelLarge)
                        Text(
                            listOfNotNull(d?.let { Format.duration(it.toLong()) }, Urls.host(ui.url)).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                    }
                    PillButton("Download", { vm.download(isNative) }, icon = QtIcons.Download)
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fetcher.webView = null
            vm.pageFetch = null
            webView?.run { stopLoading(); destroy() }
        }
    }
}
