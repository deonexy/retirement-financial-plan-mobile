pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "retirement-financial-plan-mobile-android"
include(":domain")
if (providers.gradleProperty("skipAndroidApp").orNull != "true") {
    include(":app")
}
