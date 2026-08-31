import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKotlin)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

val liveBaseUrl = "https://api.lyashukfit.ru/"
val liveChatWebSocketUrl = "wss://api.lyashukfit.ru/ws/chat"

val debugBaseUrl = providers.gradleProperty("trainer.debugBaseUrl").getOrElse(liveBaseUrl)
val debugChatWebSocketUrl = providers.gradleProperty("trainer.debugChatWebSocketUrl")
    .getOrElse(liveChatWebSocketUrl)

val appVersionName = providers.gradleProperty("trainer.versionName").getOrElse("0.1.0")
val appVersionCode = providers.gradleProperty("trainer.versionCode").getOrElse("1").toInt()

val keystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun signingSetting(property: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(property)
        ?: providers.environmentVariable(environmentVariable).orNull

android {
    namespace = "app.trainer.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.trainer.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingSetting("storeFile", "TRAINER_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = signingSetting("storePassword", "TRAINER_KEYSTORE_PASSWORD")
                keyAlias = signingSetting("keyAlias", "TRAINER_KEY_ALIAS")
                keyPassword = signingSetting("keyPassword", "TRAINER_KEY_PASSWORD")
            }
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
            buildConfigField("String", "CHAT_WEB_SOCKET_URL", "\"$debugChatWebSocketUrl\"")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            buildConfigField("String", "BASE_URL", "\"$liveBaseUrl\"")
            buildConfigField("String", "CHAT_WEB_SOCKET_URL", "\"$liveChatWebSocketUrl\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDir("src/main/kotlin")
        }
        getByName("test") {
            kotlin.srcDir("src/test/kotlin")
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(compose.runtime)
    implementation(compose.foundation)

    testImplementation(libs.koin.test)
    testImplementation(libs.multiplatform.settings)
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.kotlinx.collections.immutable)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqldelight.android.driver)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
