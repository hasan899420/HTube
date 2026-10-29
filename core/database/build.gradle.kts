plugins {
    alias(libs.plugins.htube.android.library)
    alias(libs.plugins.htube.android.room)
}

dependencies {
    api(projects.core.model)
}
