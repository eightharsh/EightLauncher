import java.util.Properties

plugins {
    alias(libs.plugins.eight.android.application)
    alias(libs.plugins.eight.android.compose)
    alias(libs.plugins.eight.android.composeui)
    alias(libs.plugins.eight.android.hilt)
    alias(libs.plugins.eight.android.testing)
    alias(libs.plugins.eight.android.room)
    alias(libs.plugins.kotlin.serialization)
}

val personalSigning = Properties().apply {
    val propertiesFile = rootProject.file(".signing/signing.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
}

val appVersion = "1.0"

android {
    namespace = "com.eightharsh.eightharsh"

    defaultConfig {
        applicationId = "com.eightharsh.eightharsh"
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 4
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "app_version", appVersion)
        resValue("string", "app_name", "eight")

        buildConfigField("String", "APP_VERSION", "\"$appVersion\"")
        buildConfigField("String", "APP_NAME", "\"eight\"")
    }
    signingConfigs {
        if (personalSigning.isNotEmpty()) {
            create("personal") {
                storeFile = rootProject.file(".signing/eightharsh.jks")
                storePassword = personalSigning.getProperty("storePassword")
                keyAlias = "eightharsh"
                keyPassword = personalSigning.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "eight")
            buildConfigField("String", "APP_NAME", "\"eight\"")
        }
        release {
            signingConfig = signingConfigs.findByName("personal")
            resValue("string", "app_name", "eight")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.bundles.androidx.core)
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.navigation3)
    implementation(libs.bundles.hilt)

    // WorkManager (with Hilt-injected workers)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    // JSON Parsing
    implementation(libs.gson)

    // Testing Libraries
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging Tools
    debugImplementation(libs.androidx.ui.test.manifest)

    // Modules
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:theme"))
    implementation(project(":feature:homescreen"))
    implementation(project(":feature:workapps"))
    implementation(project(":feature:privatespace"))
    implementation(project(":feature:securefolder"))
    implementation(project(":feature:screentime"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:appslist"))
    implementation(project(":feature:newwidgets"))
    implementation(project(":feature:onboarding"))
}