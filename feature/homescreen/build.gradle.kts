plugins {
    alias(libs.plugins.eight.android.feature)
    alias(libs.plugins.eight.android.compose)
    alias(libs.plugins.eight.android.composeui)
}

android {
    namespace = "com.eightharsh.eightharsh.feature.homescreen"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:model"))

    implementation(libs.androidx.appcompat)
}
