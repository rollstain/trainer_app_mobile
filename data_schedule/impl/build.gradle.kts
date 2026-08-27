plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKmpLibrary.get().pluginId)
    id(libs.plugins.kotlinSerialization.get().pluginId)
}

base {
    archivesName = "data_schedule-impl"
}

kotlin {
    android {
        namespace = "app.trainer.data_schedule.impl"
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
            baseName = "DataScheduleImpl"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":data_schedule:api"))
            implementation(project(":core_network:api"))
            implementation(project(":core_logger"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
    }
}
