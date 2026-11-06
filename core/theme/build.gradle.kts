plugins {
    alias(libs.plugins.eight.android.library)
    alias(libs.plugins.eight.android.compose)
    alias(libs.plugins.eight.android.composeui)
    alias(libs.plugins.eight.android.hilt)
    alias(libs.plugins.eight.android.testing)
}

android {
    namespace = "com.eightharsh.eightharsh.core.theme"
}

dependencies {
    implementation(libs.compose.google.fonts)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.google.android.material)
    implementation(project(":core:domain"))
}
