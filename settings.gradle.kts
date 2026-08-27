rootProject.name = "trainer_app"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":core_entities")
include(":core_logger")
include(":uikit")
include(":core_strings")
include(":navigation")
include(":core_base_feature")
include(":core_network:api")
include(":core_network:impl")
include(":core_database")
include(":core_media")
include(":data_chat:api")
include(":data_chat:impl")
include(":data_schedule:api")
include(":data_schedule:impl")
include(":data_auth:api")
include(":data_auth:impl")
include(":data_clients:api")
include(":data_clients:impl")
include(":data_push:api")
include(":data_push:impl")
include(":data_training_log:api")
include(":data_training_log:impl")
include(":data_progress:api")
include(":data_progress:impl")
include(":data_profile:api")
include(":data_profile:impl")
include(":data_program:api")
include(":data_program:impl")
include(":feature_schedule")
include(":feature_chat")
include(":feature_client_card")
include(":feature_training_log")
include(":feature_progress")
include(":feature_account")
include(":feature_home")
include(":composeApp")
include(":androidApp")
