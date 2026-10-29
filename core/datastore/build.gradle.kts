plugins {
    alias(libs.plugins.htube.android.library)
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.datastore.preferences)
}
