package com.mdhasan.htube.di

import com.mdhasan.htube.navigation.UrlRouter
import com.mdhasan.htube.BuildConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("updateUrl")) { BuildConfig.UPDATE_API_URL }
    single { UrlRouter(get(), get()) }
}
