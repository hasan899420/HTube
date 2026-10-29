package com.mdhasan.htube.core.data

import com.mdhasan.htube.core.database.databaseModule
import com.mdhasan.htube.core.datastore.datastoreModule
import com.mdhasan.htube.core.extractor.extractorModule
import org.koin.dsl.module

val dataModule = module {
    includes(databaseModule, datastoreModule, extractorModule)
    single<MediaRepository> { MediaRepositoryImpl(get()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<BrowserRepository> { BrowserRepositoryImpl(get(), get(), get()) }
    single<SearchHistoryRepository> { SearchHistoryRepositoryImpl(get()) }
}
