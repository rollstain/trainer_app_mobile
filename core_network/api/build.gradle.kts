plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKmpLibrary.get().pluginId)
    id(libs.plugins.kotlinSerialization.get().pluginId)
}

kotlin {
    android {
        namespace = "app.trainer.core_network.api"
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
            baseName = "CoreNetworkApi"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            api(project(":core_entities"))
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines)
        }
    }
}
