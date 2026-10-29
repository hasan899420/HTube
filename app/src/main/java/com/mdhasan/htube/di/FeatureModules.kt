package com.mdhasan.htube.di

import com.mdhasan.htube.feature.browser.browserModule
import com.mdhasan.htube.feature.home.homeModule
import com.mdhasan.htube.feature.library.libraryModule
import com.mdhasan.htube.feature.search.searchModule
import com.mdhasan.htube.feature.settings.settingsModule
import com.mdhasan.htube.feature.tools.toolsModule
import com.mdhasan.htube.feature.video.videoModule

/** Add a feature's Koin module here when adding a new feature module. */
val featureModules = listOf(appModule, homeModule, searchModule, videoModule, libraryModule,
    browserModule, settingsModule, toolsModule,
)
