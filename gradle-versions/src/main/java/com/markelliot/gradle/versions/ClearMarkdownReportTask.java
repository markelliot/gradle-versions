package com.markelliot.gradle.versions;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskAction;

public abstract class ClearMarkdownReportTask extends DefaultTask {

    public ClearMarkdownReportTask() {
        // force this task to always run
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
        if (!getProject().equals(getProject().getRootProject())) {
            getLogger().warn("Can only run updateVersionsProps on the root project");
            return;
        }

        Reports.clearMarkdownReport(getProject().getProjectDir());
    }
}
