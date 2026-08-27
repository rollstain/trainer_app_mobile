plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKmpLibrary.get().pluginId)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    android {
        namespace = "app.trainer.composeapp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":core_strings"))
            api(project(":navigation"))
            api(project(":uikit"))
            api(project(":core_base_feature"))
            api(project(":core_network:api"))
            api(project(":core_network:impl"))
            api(project(":core_database"))
            api(project(":core_logger"))
            api(project(":data_auth:api"))
            api(project(":data_auth:impl"))
            api(project(":data_push:api"))
            api(project(":data_push:impl"))
            api(project(":data_training_log:api"))
            api(project(":data_training_log:impl"))
            api(project(":data_profile:api"))
            api(project(":data_profile:impl"))
            api(project(":data_progress:api"))
            api(project(":data_progress:impl"))
            api(project(":data_clients:api"))
            api(project(":data_clients:impl"))
            api(project(":data_chat:api"))
            api(project(":data_chat:impl"))
            api(project(":data_schedule:api"))
            api(project(":data_schedule:impl"))
            api(project(":feature_account"))
            api(project(":data_program:api"))
            api(project(":data_program:impl"))
            api(project(":feature_chat"))
            api(project(":feature_home"))
            api(project(":feature_client_card"))
            api(project(":feature_training_log"))
            api(project(":feature_progress"))
            api(project(":core_media"))
            api(project(":feature_schedule"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(compose.runtime)
        }
    }
}

