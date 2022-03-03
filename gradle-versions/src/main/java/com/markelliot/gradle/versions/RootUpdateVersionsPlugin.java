package com.markelliot.gradle.versions;

import java.util.Map;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

public final class RootUpdateVersionsPlugin implements Plugin<Project> {
    private static final String VERSIONS_PROPS = "versions.props";
    private static final String ALL_REPORTS = "allReports";

    @Override
    public void apply(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new IllegalStateException("Plugin must be applied onto root project");
        }

        Configuration reportConfiguration = createReportConfiguration(project);
        project.getAllprojects()
                .forEach(
                        subProject -> {
                            subProject.getPluginManager().apply(UpdateVersionsPlugin.class);
                            Dependency dependency =
                                    project.getDependencies()
                                            .project(
                                                    Map.of(
                                                            "path",
                                                            subProject.getPath(),
                                                            "configuration",
                                                            UpdateVersionsPlugin.NEW_VERSIONS));
                            project.getDependencies()
                                    .add(reportConfiguration.getName(), dependency);
                        });

        project.getTasks()
                .register(
                        "updateVersionsProps",
                        UpdateVersionsPropsTask.class,
                        task -> {
                            task.setDescription(
                                    "Uses result of checkNewVersions task to update versions.props");

                            task.getVersionsProps().set(project.file(VERSIONS_PROPS));
                            task.getReports().from(reportConfiguration);
                        });

        project.getTasks()
                .register(
                        "updatePlugins",
                        UpdatePluginsTask.class,
                        task -> {
                            task.getReports().from(reportConfiguration);
                            task.setDescription(
                                    "Uses result of checkNewVersions task to update buildscript plugin blocks");
                        });
        project.getTasks()
                .create("updateGradleWrapper", UpdateGradleWrapperTask.class)
                .setDescription("Uses result of checkNewGradleVersion to update Gradle wrapper");

        Task gradleTask =
                project.getTasks().create("checkNewGradleVersion", CheckNewGradleVersionTask.class);
        gradleTask.setDescription("Checks for and reports on existence of a new Gradle version");
        project.getTasks().getByName("checkNewVersions").dependsOn(gradleTask);
    }

    private static Configuration createReportConfiguration(Project project) {
        return project.getConfigurations()
                .create(
                        ALL_REPORTS,
                        conf -> {
                            conf.setCanBeResolved(true);
                            conf.setCanBeConsumed(true);
                            conf.setVisible(false);
                        });
    }
}
