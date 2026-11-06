package com.eightharsh.eightharsh

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("eight.android.library")
                apply("eight.android.composeui")
                apply("eight.android.hilt")
                apply("eight.android.testing")
            }

            dependencies {
                add("implementation", libs.findBundle("hilt").get())
            }
        }
    }
}
