plugins {
    alias(libs.plugins.htube.android.feature)
}

dependencies {
    implementation(libs.androidx.documentfile)
    implementation(projects.core.media)
    implementation(libs.coil.video)
    implementation(projects.core.download)
}
