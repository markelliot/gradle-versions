package com.markelliot.gradle.versions.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableGradleUpdateRec.class)
@JsonSerialize(as = ImmutableGradleUpdateRec.class)
public interface GradleUpdateRec {
    String currentVersion();

    String latestVersion();

    String distributionUrl();
}
