plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKmpLibrary.get().pluginId)
    id(libs.plugins.kotlinSerialization.get().pluginId)
}

base {
    archivesName = "data_training_log-api"
}

kotlin {
    android {
        namespace = "app.trainer.data_training_log.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DataTrainingLogApi"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":core_entities"))
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines)
        }
    }
}
