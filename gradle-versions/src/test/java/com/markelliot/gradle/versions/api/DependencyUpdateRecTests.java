package com.markelliot.gradle.versions.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class DependencyUpdateRecTests {
    @Test
    void test_handleEmptyCurrentVersion() {
        DependencyUpdateRec rec =
                YamlSerDe.deserialize(
                        """
                        group: group
                        name: name
                        latestVersion: latestVersion
                        """,
                        DependencyUpdateRec.class);

        assertThat(rec.currentVersion()).isNullOrEmpty();
    }
}
