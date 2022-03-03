package com.markelliot.gradle.versions;

import com.google.common.annotations.VisibleForTesting;
import com.jakewharton.nopen.annotation.Open;
import com.markelliot.gradle.versions.api.GradleUpdateReport;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Open
public class UpdateGradleWrapperTask extends DefaultTask {
    private static final String WRAPPER_PROPS = "gradle/wrapper/gradle-wrapper.properties";

    private static final Logger log = LoggerFactory.getLogger(UpdateGradleWrapperTask.class);

    @TaskAction
    public void taskAction() {
        if (!getProject().equals(getProject().getRootProject())) {
            log.warn("Can only run updateGradle on the root project");
            return;
        }

        Reports.loadGradleUpdateReport(getProject().getBuildDir())
                .ifPresent(this::applyGradleUpdate);
    }

    private void applyGradleUpdate(GradleUpdateReport gur) {
        File file = new File(getProject().getProjectDir(), WRAPPER_PROPS);
        if (file.exists()) {
            System.out.println(
                    "Updating Gradle wrapper "
                            + gur.gradle().currentVersion()
                            + " -> "
                            + gur.gradle().latestVersion());
            try {
                String content = Files.readString(file.toPath());
                Files.writeString(
                        file.toPath(),
                        updateDistributionUrl(content, gur.gradle().distributionUrl()),
                        StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final Pattern DISTURL = Pattern.compile("distributionUrl=.*");

    @VisibleForTesting
    static String updateDistributionUrl(String originalFile, String url) {
        Matcher matcher = DISTURL.matcher(originalFile);
        if (matcher.find()) {
            return matcher.replaceFirst(
                    Matcher.quoteReplacement("distributionUrl=" + escapeUrl(url)));
        }
        return originalFile;
    }

    @VisibleForTesting
    static String escapeUrl(String url) {
        return url.replaceAll(Pattern.quote(":"), "\\\\:");
    }
}
