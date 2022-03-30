package com.markelliot.gradle.versions;

import static com.markelliot.gradle.versions.UpdatePluginsTask.applyUpdate;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class UpdatePluginsTaskTests {
    @Test
    public void testApplyUpdate() {
        // gradle/groovy
        assertThat(applyUpdate("id \"com.foo.bar\" version \"0.1.0\"", "com.foo.bar", "0.2.0"))
                .isEqualTo("id \"com.foo.bar\" version \"0.2.0\"");
        assertThat(applyUpdate("id 'com.foo.bar' version '0.1.0'", "com.foo.bar", "0.2.0"))
                .isEqualTo("id 'com.foo.bar' version '0.2.0'");
        // gradle/kotlin
        assertThat(applyUpdate("id(\"com.foo.bar\") version \"0.1.0\"", "com.foo.bar", "0.2.0"))
                .isEqualTo("id(\"com.foo.bar\") version \"0.2.0\"");
        // no match
        assertThat(applyUpdate("id \"com.foo.baz\" version \"0.1.0\"", "com.foo.bar", "0.2.0"))
                .isEqualTo("id \"com.foo.baz\" version \"0.1.0\"");
        assertThat(applyUpdate("id 'com.foo.baz' version '0.1.0'", "com.foo.bar", "0.2.0"))
                .isEqualTo("id \"com.foo.baz\" version \"0.1.0\"");
    }
}
