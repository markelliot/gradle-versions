plugins {
    `groovy`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.1"
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
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core")
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
            tags = listOf("versions")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            suppressPomMetadataWarningsFor("javadocElements")
            pom {
                name.set("gradle-versions")
                description.set("Gradle plugin that helps with updating Gradle dependencies.")
                url.set("https://github.com/markelliot/gradle-versions")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("markelliot")
                        name.set("Mark Elliot")
                        email.set("markelliot@users.noreply.github.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/markelliot/gradle-versions.git")
                    developerConnection.set("scm:git:https://github.com/markelliot/gradle-versions.git")
                    url.set("https://github.com/markelliot/gradle-versions")
                }
            }
        }
    }
}
