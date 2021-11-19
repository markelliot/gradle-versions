package com.markelliot.gradle.versions;

import com.google.common.annotations.VisibleForTesting;
import com.jakewharton.nopen.annotation.Open;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

@Open
public class UpdateGradleWrapperTask extends DefaultTask {
    @TaskAction
    public void taskAction() {
        if (!getProject().equals(getProject().getRootProject())) {
            getLogger().warn("Can only run updateGradle on the root project");
            return;
        }

        Reports.loadGradleReport(getProject().getBuildDir())
                .ifPresent(
                        gur -> {
                            File file =
                                    new File(
                                            getProject().getProjectDir(),
                                            "gradle/wrapper/gradle-wrapper.properties");
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
                                            updateDistributionUrl(
                                                    content, gur.gradle().distributionUrl()),
                                            StandardOpenOption.TRUNCATE_EXISTING);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
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
