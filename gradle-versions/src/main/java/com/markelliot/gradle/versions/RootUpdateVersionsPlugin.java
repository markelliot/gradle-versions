package com.markelliot.gradle.versions;

import java.util.Map;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RootUpdateVersionsPlugin implements Plugin<Project> {
    private static final String VERSIONS_PROPS = "versions.props";
    private static final String ALL_REPORTS = "allReports";

    private static final Logger log = LoggerFactory.getLogger(RootUpdateVersionsPlugin.class);

    @Override
    public void apply(Project project) {
        if (!project.equals(project.getRootProject())) {
            log.warn("Can only apply com.markelliot.versions on the root project");
            return;
        }

        Configuration reportConfiguration = createReportConfiguration(project);
        project.allprojects(subProject -> {
            subProject.getPluginManager().apply(UpdateVersionsPlugin.class);
            Dependency dependency =
                    project.getDependencies()
                            .project(
                                    Map.of(
                                            "path",
                                            subProject.getPath()));
            reportConfiguration.getDependencies().add(dependency);
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
                .register("updateGradleWrapper", UpdateGradleWrapperTask.class, task -> {
                    task.setDescription("Uses result of checkNewGradleVersion to update Gradle wrapper");
                });

        TaskProvider<CheckNewGradleVersionTask> gradleTask = project.getTasks().register(
                "checkNewGradleVersion",
                CheckNewGradleVersionTask.class,
                task -> {
                    task.setDescription("Checks for and reports on existence of a new Gradle version");
                });
        project.getTasks().named("checkNewVersions").configure(task -> task.dependsOn(gradleTask));

        project.getTasks().register("updateAll", task -> {
            task.dependsOn(
                    project.getTasks().getByName("updateGradleWrapper"),
                    project.getTasks().getByName("updatePlugins"),
                    project.getTasks().getByName("updateVersionsProps"));
        });
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
