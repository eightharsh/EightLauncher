plugins {
    alias(libs.plugins.eight.android.library)
    alias(libs.plugins.eight.android.compose)
    alias(libs.plugins.eight.android.composeui)
    alias(libs.plugins.eight.android.testing)
}

android {
    namespace = "com.eightharsh.eightharsh.core.ui"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(project(":core:theme"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.reorderable)
}
