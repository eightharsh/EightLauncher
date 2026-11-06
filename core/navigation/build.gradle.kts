plugins {
    alias(libs.plugins.eight.android.library)
    alias(libs.plugins.eight.android.compose)
    alias(libs.plugins.eight.android.composeui)
    alias(libs.plugins.eight.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.eightharsh.eightharsh.core.navigation"
}

dependencies {
    implementation(libs.bundles.navigation3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
