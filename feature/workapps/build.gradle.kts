plugins {
    alias(libs.plugins.eight.android.feature)
}

android {
    namespace = "com.eightharsh.eightharsh.feature.workapps"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:theme"))
}
