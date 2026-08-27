plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKmpLibrary.get().pluginId)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    android {
        namespace = "app.trainer.feature_schedule"
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
            baseName = "FeatureSchedule"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":core_strings"))
            api(project(":data_schedule:api"))
            api(project(":data_profile:api"))
            api(project(":data_clients:api"))
            api(project(":core_base_feature"))
            api(project(":navigation"))
            api(project(":uikit"))
            implementation(project(":core_entities"))
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(compose.runtime)
        }
    }
}

