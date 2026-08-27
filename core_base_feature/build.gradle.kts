plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKmpLibrary.get().pluginId)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    android {
        namespace = "app.trainer.core_base_feature"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CoreBaseFeature"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":core_strings"))
            api(compose.runtime)
            api(libs.lifecycle.viewmodel)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.koin.core)
            api(project(":core_logger"))
            api(project(":core_entities"))
            api(project(":uikit"))
        }
    }
}

