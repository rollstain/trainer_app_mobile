plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKmpLibrary.get().pluginId)
}

kotlin {
    android {
        namespace = "app.trainer.data_chat.api"
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
            baseName = "DataChatApi"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":core_entities"))
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines)
        }
    }
}
