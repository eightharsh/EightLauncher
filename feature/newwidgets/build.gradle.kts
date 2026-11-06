plugins {
    alias(libs.plugins.eight.android.feature)
}

android {
    namespace = "com.eightharsh.eightharsh.feature.newwidgets"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:theme"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
}