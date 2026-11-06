plugins {
    alias(libs.plugins.eight.android.library)
    alias(libs.plugins.eight.android.testing)
}

android {
    namespace = "com.eightharsh.eightharsh.core.model"
}

dependencies {
    implementation(libs.androidx.annotation)
}
