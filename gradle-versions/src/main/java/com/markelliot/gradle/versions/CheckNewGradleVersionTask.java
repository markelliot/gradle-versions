package com.markelliot.gradle.versions;

import com.jakewharton.nopen.annotation.Open;
import com.markelliot.gradle.versions.GradleVersions.ReleaseChannel;
import com.markelliot.gradle.versions.api.GradleUpdateRec;
import com.markelliot.gradle.versions.api.ImmutableGradleUpdateRec;
import com.markelliot.gradle.versions.api.ImmutableGradleUpdateReport;
import java.util.Optional;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GradleVersion;

@Open
public class CheckNewGradleVersionTask extends DefaultTask {

    public CheckNewGradleVersionTask() {
        // always stale
        getOutputs()
                .upToDateWhen(
                        new Spec<Task>() {
                            @Override
                            public boolean isSatisfiedBy(Task task) {
                                return false;
                            }
                        });
    }

    @TaskAction
    public final void taskAction() {
        Optional<GradleUpdateRec> gradleUpdate = getGradleUpdate();
        gradleUpdate.ifPresent(
                gu -> {
                    System.out.println(
                            "A new version of Gradle is available: "
                                    + gu.currentVersion()
                                    + " -> "
                                    + gu.latestVersion());
                    Reports.writeGradleUpdateReport(
                            getProject().getBuildDir(),
                            ImmutableGradleUpdateReport.builder()
                                    .project(getProject().getPath())
                                    .gradle(gu)
                                    .build());
                });
    }

    private Optional<GradleUpdateRec> getGradleUpdate() {
        GradleVersion current = GradleVersion.current();
        return GradleVersions.forChannel(ReleaseChannel.CURRENT)
                .filter(v -> current.compareTo(GradleVersion.version(v.version())) < 0)
                .map(
                        v ->
                                ImmutableGradleUpdateRec.builder()
                                        .currentVersion(current.getVersion())
                                        .latestVersion(v.version())
                                        .distributionUrl(v.distributionUrl())
                                        .build());
    }
}
