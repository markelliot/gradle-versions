package com.markelliot.gradle.versions;

import com.jakewharton.nopen.annotation.Open;
import com.markelliot.gradle.versions.api.SerDe;
import com.markelliot.gradle.versions.api.UpdateReport;
import com.markelliot.gradle.versions.props.VersionsProps;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

@Open
public class UpdateVersionsPropsTask extends DefaultTask {
    private static final String VERSIONS_PROPS = "versions.props";

    @TaskAction
    public final void taskAction() {
        if (!getProject().equals(getProject().getRootProject())) {
            getLogger().warn("updateVersionsProps task may only be called from the root project");
            return;
        }

        // collect all the update recommendations.
        List<UpdateReport> reports = new ArrayList<>();
        getProject()
                .allprojects(
                        proj -> {
                            Path path =
                                    Paths.get(
                                            proj.getBuildDir().getPath(),
                                            CheckNewVersionsTask.REPORT_DIRNAME,
                                            CheckNewVersionsTask.REPORT_YML);
                            if (path.toFile().exists()) {
                                getLogger().info("Found reports.yml for " + proj);
                                reports.add(SerDe.deserialize(path, UpdateReport.class));
                            }
                        });

        // merge recommendations
        Map<String, String> updateRecs = mergeDependencyUpdates(reports);

        // update versions.props
        File versionPropsFile = getProject().file(VERSIONS_PROPS);
        VersionsProps versionsProps = VersionsProps.from(versionPropsFile);
        updateRecs.forEach(versionsProps::update);
        versionsProps.to(versionPropsFile);
    }

    private Map<String, String> mergeDependencyUpdates(List<UpdateReport> reports) {
        Map<String, String> updateRecs = new HashMap<>();
        reports.stream()
                .flatMap(report -> report.dependencyUpdates().stream())
                .forEach(
                        rec -> {
                            String identifier = rec.group() + ":" + rec.name();
                            if (updateRecs.containsKey(identifier)) {
                                if (!updateRecs.get(identifier).equals(rec.latestVersion())) {
                                    getLogger()
                                            .warn(
                                                    "Found conflicting version recommendation for '"
                                                            + identifier
                                                            + "'");

                                    // remove so we ignore the conflict
                                    updateRecs.remove(identifier);
                                }
                            } else {
                                updateRecs.put(identifier, rec.latestVersion());
                            }
                        });
        return updateRecs;
    }
}
