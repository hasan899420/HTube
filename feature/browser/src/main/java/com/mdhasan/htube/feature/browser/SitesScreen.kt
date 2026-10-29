package com.mdhasan.htube.feature.browser

import com.mdhasan.htube.core.designsystem.QtIconButton
import com.mdhasan.htube.core.designsystem.icon.QtIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdhasan.htube.core.common.Urls
import com.mdhasan.htube.core.ui.BrandTile
import com.mdhasan.htube.core.ui.brandFor
import com.mdhasan.htube.core.designsystem.QtTopBar
import com.mdhasan.htube.core.ui.ConfirmDialog
import com.mdhasan.htube.core.ui.EmptyBox
import org.koin.compose.viewmodel.koinViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun SiteIcon(name: String, url: String = name) {
    BrandTile(if (brandFor(name) != null) name else url, size = 46.dp, label = name)
}

// VidMate-style curated 18+ section: favicon tiles only, no preview thumbnails.
// The section is hidden by default on every visit to this screen; it is
// shown only for this visit after the age dialog is confirmed, and Hide
// hides it again at once. Global + Japanese lists below are exactly the
// links MD sent 2026-10-30; the "More" list keeps the earlier video names
// that are not already in those two lists (RedTube URL updated to the
// .net address MD sent, instead of the earlier .com guess).
private val globalSites = listOf(
    "Pornhub" to "https://www.pornhub.com/",
    "XVideos" to "https://www.xvideos.com",
    "xHamster" to "https://xhamster.com/",
    "XNXX" to "https://www.xnxx.com/",
    "Brazzers" to "https://www.brazzers.com/",
    "RedTube" to "https://redtube.net/",
    "Tube8" to "https://www.tube8.com/",
    "XVideos ES" to "https://www.xvideos.es/",
)
private val japaneseSites = listOf(
    "MissAV" to "https://missav.ws",
    "JAV Guru" to "https://jav.guru/",
    "Jable" to "https://jable.tv/",
    "NJAVTV" to "https://njavtv.com/",
)
private val moreAdultSites = listOf(
    "Desigirl" to "https://www.desigirl.com",
    "Dateme" to "https://www.dateme.com",
    "TXXX" to "https://www.txxx.com",
    "Eporner" to "https://www.eporner.com",
    "PornGo" to "https://www.porngo.com",
    "XVideos Plus" to "https://www.xvideos.com",
    "YouPorn" to "https://www.youporn.com",
    "Thumbzilla" to "https://www.thumbzilla.com",
    "Luxe Video" to "https://www.luxevideo.com",
    "Pornxxx" to "https://www.pornxxx.com",
    "Spankbang" to "https://www.spankbang.com",
    "NoodleMagazine" to "https://www.noodlemagazine.com",
)

@Composable
private fun AdultSiteIcon(name: String, url: String) {
    // Site favicon (brand mark) so the site is recognisable; if it fails
    // to load, the initial letter underneath stays visible.
    val host = url.substringAfter("://").substringBefore("/").removePrefix("www.")
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
        )
        AsyncImage(
            model = "https://www.google.com/s2/favicons?domain=$host&sz=128",
            contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(46.dp),
        )
    }
}

@Composable
private fun AdultSiteGrid(title: String, sites: List<Pair<String, String>>, onOpen: (String) -> Unit) {
    // MD wants these INSIDE the app, not in an external browser, so tap
    // opens HTube's in-app browser (BrowserRoute via onOpen). The in-app
    // browser's video detector then shows the Download bar on video pages,
    // so the video can be downloaded from inside the app.
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
    sites.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            row.forEach { (name, url) ->
                Column(
                    Modifier.weight(1f).clickable { onOpen(url) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AdultSiteIcon(name, url)
                    Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AdultSitesSection(onOpen: (String) -> Unit) {
    // rememberSaveable (not remember) so going to another screen and back
    // does not reset this to hidden and force Show Sites + Confirm again.
    var adultShown by rememberSaveable { mutableStateOf(false) }
    var showAgeDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("18+ sites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (adultShown) {
                Button(onClick = { adultShown = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Hide") }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!adultShown) {
            Text("Some sites have been hidden due to your content settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showAgeDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Show Sites") }
        } else {
            Text("Tap a site to open it inside HTube. On a video page, tap play and use the Download bar in the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AdultSiteGrid("Global Sites", globalSites, onOpen)
            AdultSiteGrid("Japanese Sites", japaneseSites, onOpen)
            AdultSiteGrid("More 18+ sites", moreAdultSites, onOpen)
        }
    }
    if (showAgeDialog) {
        AlertDialog(
            onDismissRequest = { showAgeDialog = false },
            title = { Text("Age Verification") },
            text = { Text("These websites contain restricted content, including nudity and sexually explicit material. You confirm that you are over 18 years of age, or an adult in the jurisdiction from which you access this site.") },
            confirmButton = {
                Button(onClick = { showAgeDialog = false; adultShown = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showAgeDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SitesScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onHistory: () -> Unit) {
    val vm = koinViewModel<SitesViewModel>()
    val sites by vm.sites.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Set<Long>?>(null) }
    var adding by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        val d = deleting
        if (d == null) QtTopBar("Sites", onBack) {
            QtIconButton(QtIcons.History, "History", onHistory)
            QtIconButton(QtIcons.Delete, "Delete", { deleting = emptySet() })
            QtIconButton(QtIcons.Add, "Add", { adding = true })
        } else QtTopBar("${d.size} selected", { deleting = null }) {
            TextButton(enabled = d.isNotEmpty(), onClick = { vm.delete(d.toList()); deleting = null }) { Text("Delete") }
        }
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            item { AdultSitesSection(onOpen) }
            items(sites.size) { i ->
                val s = sites[i]
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val d = deleting
                        if (d != null) deleting = if (s.id in d) d - s.id else d + s.id else onOpen(s.url)
                    }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SiteIcon(s.name, s.url)
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text(s.name, style = MaterialTheme.typography.bodyLarge)
                        Text(if (s.url.startsWith("htube://")) "Built-in tool" else s.url, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    deleting?.let { d -> Checkbox(s.id in d, { _ -> deleting = if (s.id in d) d - s.id else d + s.id }) }
                }
            }
        }
    }
    if (adding) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add site") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(url, { url = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && Urls.looksLikeUrl(url), onClick = {
                    vm.add(name.trim(), Urls.normalize(url)); adding = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = koinViewModel<SitesViewModel>()
    val history by vm.history.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        QtTopBar("History", onBack) { if (history.isNotEmpty()) TextButton(onClick = { confirm = true }) { Text("Clear all") } }
    }) { pad ->
        if (history.isEmpty()) EmptyBox("No browsing history", modifier = Modifier.fillMaxSize().padding(pad), icon = QtIcons.History)
        else LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(history, key = { it.id }) { h ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(h.url) }.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text(h.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Urls.host(h.url).orEmpty() + " · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(h.visitedAt)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    QtIconButton(QtIcons.Close, "Remove", { vm.deleteHistory(h.id) })
                }
            }
        }
    }
    if (confirm) ConfirmDialog("Clear history?", "All browsing history will be removed.", "Clear", { vm.clearHistory() }, { confirm = false })
}

@Composable
fun BookmarksScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = koinViewModel<SitesViewModel>()
    val list by vm.bookmarks.collectAsStateWithLifecycle()
    Scaffold(topBar = { QtTopBar("Saved", onBack) }) { pad ->
        if (list.isEmpty()) EmptyBox("Nothing saved yet", "Use “Save page” or “Watch later” to keep pages here", Modifier.fillMaxSize().padding(pad))
        else LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(list, key = { it.id }) { b ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(b.url) }.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text(b.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(Urls.host(b.url).orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    QtIconButton(QtIcons.Close, "Remove", { vm.removeBookmark(b.url) })
                }
            }
        }
    }
}
