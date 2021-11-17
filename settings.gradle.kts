rootProject.name = "gradle-versions-root"

include("gradle-versions")

/* // for development/self-testing:
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.markelliot.versions") {
                useModule("com.markelliot.gradle.versions:gradle-versions:${requested.version}")
            }
        }
    }
}
*/
