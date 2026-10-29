package com.mdhasan.htube.core.extractor

import com.mdhasan.htube.core.model.ChannelDetails
import com.mdhasan.htube.core.model.Comment
import com.mdhasan.htube.core.model.ItemKind
import com.mdhasan.htube.core.model.MediaItem
import com.mdhasan.htube.core.model.MediaSection
import com.mdhasan.htube.core.model.Page
import com.mdhasan.htube.core.model.PlaylistDetails
import com.mdhasan.htube.core.model.SearchFilter
import com.mdhasan.htube.core.model.VideoDetails

/** Platform-agnostic contract for online media lookups (search, feeds, stream info). */
interface MediaExtractor {
    fun canHandle(url: String): Boolean
    fun linkKind(url: String): ItemKind?
    suspend fun suggestions(query: String): List<String>
    suspend fun search(query: String, filter: SearchFilter, page: Any? = null): Page<MediaItem>
    suspend fun trending(page: Any? = null): Page<MediaItem>
    suspend fun musicHome(): List<MediaSection>
    suspend fun details(url: String): VideoDetails
    suspend fun comments(url: String, page: Any? = null): Page<Comment>
    suspend fun channel(url: String, page: Any? = null): ChannelDetails
    suspend fun playlist(url: String, page: Any? = null): PlaylistDetails
}
