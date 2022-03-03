package com.markelliot.gradle.versions

import nebula.test.IntegrationSpec

class UpdateVersionsPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
        ${applyPlugin(RootUpdateVersionsPlugin)}

        allprojects {
            apply plugin: 'java-library'
            
            repositories {
                mavenCentral()
            }
        }
        """.stripIndent()
    }

    def "updateVersions - multiple projects - updates versions.props"() {
        def versionsProps = file("versions.props")
        when:
        versionsProps.text = """
        com.fasterxml.jackson.*:* = 2.12.1
        """.stripIndent()

        addSubproject("projectA", """
        dependencies {
            implementation 'com.fasterxml.jackson.core:jackson-databind:2.12.1'
        }
        """)

        then:
        def result = runTasksSuccessfully('updateVersionsProps')
        result.wasExecuted('projectA:checkNewVersions')

        !versionsProps.text.contains("2.12.1")
    }
}
