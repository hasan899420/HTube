plugins {
    alias(libs.plugins.htube.android.library)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.extractor)
    implementation(libs.youtubedl.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
}
