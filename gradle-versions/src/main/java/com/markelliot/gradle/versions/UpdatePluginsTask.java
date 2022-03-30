package com.markelliot.gradle.versions;

import com.google.common.annotations.VisibleForTesting;
import com.markelliot.gradle.versions.api.UpdateReport;
import com.markelliot.gradle.versions.api.YamlSerDe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

public abstract class UpdatePluginsTask extends DefaultTask {

    @InputFiles
    abstract ConfigurableFileCollection getReports();

    @TaskAction
    public void taskAction() {
        List<UpdateReport> reports =
                getReports().getFiles().stream()
                        .map(file -> YamlSerDe.deserialize(file.toPath(), UpdateReport.class))
                        .collect(Collectors.toUnmodifiableList());

        Map<String, String> pluginUpdates = mergePluginUpdates(reports);
        getProject()
                .allprojects(
                        proj -> {
                            File sourceFile = proj.getBuildscript().getSourceFile();
                            if (sourceFile != null && sourceFile.exists()) {
                                setNewPluginVersions(sourceFile, pluginUpdates);
                            }
                        });
    }

    private void setNewPluginVersions(File sourceFile, Map<String, String> pluginUpdates) {
        String content = read(sourceFile);
        for (Map.Entry<String, String> update : pluginUpdates.entrySet()) {
            content = applyUpdate(content, update.getKey(), update.getValue());
        }
        write(sourceFile, content);
    }

    @VisibleForTesting
    static String applyUpdate(String content, String pluginName, String pluginVersion) {
        Pattern pattern =
                Pattern.compile(
                        "id(\\s+|\\s*\\()(\"|')"
                                + Pattern.quote(pluginName)
                                + "(\"|')(\\s+|\\)\\s+)version\\s+\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            content =
                    matcher.replaceFirst(
                            "id$1$2" + pluginName + "$3$4version \"" + pluginVersion + "\"");
        }
        return content;
    }

    private static String read(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void write(File file, String content) {
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> mergePluginUpdates(List<UpdateReport> reports) {
        Map<String, String> updateRecs = new HashMap<>();
        reports.stream()
                .flatMap(report -> report.pluginUpdates().stream())
                // coordinates< for plugin-block dependencies (which is all we support) are of the
                // form:
                // <pluginId>:<pluginId>.gradle.plugin
                .filter(rec -> rec.name().endsWith(".gradle.plugin"))
                .forEach(
                        rec -> {
                            String pluginName = rec.group();
                            if (updateRecs.containsKey(pluginName)) {
                                if (!updateRecs.get(pluginName).equals(rec.latestVersion())) {
                                    getLogger()
                                            .warn(
                                                    "Found conflicting version recommendation for plugin '"
                                                            + pluginName
                                                            + "'");

                                    // remove so we ignore the conflict
                                    updateRecs.remove(pluginName);
                                }
                            } else {
                                updateRecs.put(pluginName, rec.latestVersion());
                            }
                        });
        return updateRecs;
    }
}
