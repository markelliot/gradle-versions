package com.markelliot.gradle.versions;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class ClearMarkdownReportTask extends DefaultTask {
    @TaskAction
    public final void taskAction() {
        if (!getProject().equals(getProject().getRootProject())) {
            getLogger().warn("Can only run updateVersionsProps on the root project");
            return;
        }

        Reports.clearMarkdownReport(getProject().getProjectDir());
    }
}
