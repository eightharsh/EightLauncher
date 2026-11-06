plugins {
    alias(libs.plugins.eight.android.library)
    alias(libs.plugins.eight.android.testing)
}

android {
    namespace = "com.eightharsh.eightharsh.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(project(":core:model"))
}