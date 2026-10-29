package com.mdhasan.htube.navigation

import com.mdhasan.htube.core.common.Urls
import com.mdhasan.htube.core.data.MediaRepository
import com.mdhasan.htube.core.downloadsheet.DownloadSheetController
import com.mdhasan.htube.core.model.ItemKind
import com.mdhasan.htube.core.navigation.BrowserRoute
import com.mdhasan.htube.core.navigation.ChannelRoute
import com.mdhasan.htube.core.navigation.PlaylistRoute
import com.mdhasan.htube.core.navigation.StatusSaverRoute
import com.mdhasan.htube.core.navigation.VideoRoute

/** Decides which screen opens a URL: native pages for supported links, the browser otherwise. */
class UrlRouter(private val media: MediaRepository, private val sheet: DownloadSheetController) {
    /**
     * A link the user pasted or shared means "download this": native pages open with the format sheet on top,
     * any other site opens in the browser, which pops the sheet as soon as it finds the page's video.
     */
    fun routeForPasted(raw: String): Any {
        val url = Urls.normalize(raw)
        return when (val r = routeFor(url)) {
            is VideoRoute -> r.also { sheet.open(url) }
            is BrowserRoute -> r.copy(autoDownload = true)
            else -> r
        }
    }

    fun routeFor(raw: String): Any {
        if (raw == "htube://status-saver") return StatusSaverRoute
        val url = Urls.normalize(raw)
        return when (media.linkKind(url)) {
            ItemKind.VIDEO -> VideoRoute(url)
            ItemKind.PLAYLIST -> PlaylistRoute(url)
            ItemKind.CHANNEL -> ChannelRoute(url)
            null -> BrowserRoute(url)
        }
    }
}
