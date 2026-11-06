plugins {
    alias(libs.plugins.eight.android.library)
    alias(libs.plugins.eight.android.hilt)
    alias(libs.plugins.eight.android.testing)
}

android {
    namespace = "com.eightharsh.eightharsh.core.domain"
}

dependencies {
    implementation(project(":core:model"))
}