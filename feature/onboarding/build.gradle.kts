plugins {
    alias(libs.plugins.eight.android.feature)
}
android {
    namespace = "com.eightharsh.eightharsh.feature.onboarding"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:theme"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.lottie.compose)
}
