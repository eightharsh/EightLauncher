import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.eightharsh.eightharsh.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.compiler.gradlePlugin)
    implementation(libs.hilt.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)


}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.eight.android.application.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.eight.android.library.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.eight.android.compose.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = libs.plugins.eight.android.hilt.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidHiltConventionPlugin"
        }
        register("eightTests") {
            id = libs.plugins.eight.android.testing.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidTestConventionPlugin"
        }
        register("androidRoom") {
            id = libs.plugins.eight.android.room.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidRoomConventionPlugin"
        }
        register("androidComposeUi") {
            id = libs.plugins.eight.android.composeui.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidComposeUiConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.eight.android.feature.get().pluginId
            implementationClass = "com.eightharsh.eightharsh.AndroidFeatureConventionPlugin"
        }
    }
}
