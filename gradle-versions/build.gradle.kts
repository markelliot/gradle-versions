import java.net.URI

plugins {
    `groovy`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.1.0"
}

dependencies {
    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value::annotations")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.google.guava:guava")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.netflix.nebula:nebula-test")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

gradlePlugin {
    website.set("https://github.com/markelliot/gradle-versions")
    vcsUrl.set("https://github.com/markelliot/gradle-versions")

    plugins {
        create("versions") {
            id = "com.markelliot.versions"
            displayName = "Version Update Plugin"
            description = "Creates a task that determines available dependency and plugin upgrades for a " +
                "project and produces a YAML report. Additionally provides two tasks for updating the " +
                "versions in a versions.props file (nebula.dependency-recommender or " +
                "com.palantir.consistent-versions compatible) and updating plugin versions in Gradle " +
                "plugin blocks."
            implementationClass = "com.markelliot.gradle.versions.RootUpdateVersionsPlugin"
            version = "${project.version}"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI.create("https://maven.pkg.github.com/markelliot/gradle-versions")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
