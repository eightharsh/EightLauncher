plugins {
    alias(libs.plugins.eight.android.feature)
}

android {
    namespace = "com.eightharsh.eightharsh.feature.screentime"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:theme"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
}
