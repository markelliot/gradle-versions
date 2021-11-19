package com.markelliot.gradle.versions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class UpdateGradleWrapperTaskTests {

    @Test
    public void testEscapeUrl() {
        assertThat(UpdateGradleWrapperTask.escapeUrl("https://foo.com")).isEqualTo("https\\://foo.com");
    }

    @Test
    public void testUpdateDistributionUrl() {
        String original =
                "distributionBase=GRADLE_USER_HOME\n"
                        + "distributionPath=wrapper/dists\n"
                        + "distributionUrl=https\\://services.gradle.org/distributions/gradle-7.2-bin.zip\n"
                        + "zipStoreBase=GRADLE_USER_HOME\n"
                        + "zipStorePath=wrapper/dists\n";

        String desired =
                "distributionBase=GRADLE_USER_HOME\n"
                        + "distributionPath=wrapper/dists\n"
                        + "distributionUrl=https\\://services.gradle.org/distributions/gradle-7.3-bin.zip\n"
                        + "zipStoreBase=GRADLE_USER_HOME\n"
                        + "zipStorePath=wrapper/dists\n";

        assertThat(
                        UpdateGradleWrapperTask.updateDistributionUrl(
                                original,
                                "https://services.gradle.org/distributions/gradle-7.3-bin.zip"))
                .isEqualTo(desired);
    }
}
