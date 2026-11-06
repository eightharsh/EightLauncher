pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "eight"
include(":app")
include(":core:ui")
include(":core:model")
include(":core:data")
include(":core:domain")
include(":core:common")
include(":feature:homescreen")
include(":feature:workapps")
include(":feature:privatespace")
include(":feature:securefolder")
include(":feature:settings")
include(":feature:screentime")
include(":core:di")
include(":core:theme")
include(":feature:appslist")
include(":feature:newwidgets")
include(":feature:onboarding")
include(":core:navigation")
