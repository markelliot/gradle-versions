package com.markelliot.gradle.versions.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableGradleUpdateReport.class)
@JsonSerialize(as = ImmutableGradleUpdateReport.class)
public interface GradleUpdateReport {
    String project();

    GradleUpdateRec gradle();
}
