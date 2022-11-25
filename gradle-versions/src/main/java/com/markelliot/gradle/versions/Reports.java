package com.markelliot.gradle.versions;

import com.google.common.base.Preconditions;
import com.markelliot.gradle.versions.api.GradleUpdateReport;
import com.markelliot.gradle.versions.api.YamlSerDe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public final class Reports {
    private static final String REPORT_DIRNAME = "com.markelliot.versions";
    private static final String REPORT_YML = "report.yml";
    private static final String GRADLE_REPORT_YML = "gradle-report.yml";

    private Reports() {}

    public static void writeGradleUpdateReport(File projectBuildDir, GradleUpdateReport report) {
        writeReport(projectBuildDir, GRADLE_REPORT_YML, report);
    }

    public static Optional<GradleUpdateReport> loadGradleUpdateReport(File projectBuildDir) {
        Path path = Paths.get(projectBuildDir.getPath(), REPORT_DIRNAME, GRADLE_REPORT_YML);
        if (path.toFile().exists()) {
            return Optional.of(YamlSerDe.deserialize(path, GradleUpdateReport.class));
        }
        return Optional.empty();
    }

    private static void writeReport(File projectBuildDir, String file, Object report) {
        File reportDir = mkdirIfNotExist(projectBuildDir);
        File reportFile = new File(reportDir, file);
        String reportContent = YamlSerDe.serialize(report);
        writeContentToFile(reportFile.toPath(), reportContent);
    }

    private static void writeContentToFile(Path path, String reportContent) {
        try {
            Files.writeString(
                    path,
                    reportContent,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendContentToFile(Path path, String reportContent) {
        try {
            Files.writeString(
                    path, reportContent, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearMarkdownReport(File rootProjectDir) {
        File reportDir = mkdirIfNotExist(rootProjectDir);
        try {
            Files.deleteIfExists(new File(reportDir, "report.md").toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File mkdirIfNotExist(File baseDir) {
        File reportDir = new File(baseDir, REPORT_DIRNAME);
        Preconditions.checkState(
                reportDir.exists() || reportDir.mkdirs(), "unable to make reportDir");
        return reportDir;
    }

    public static void appendMarkdownReport(File rootProjectDir, String content) {
        File reportDir = mkdirIfNotExist(rootProjectDir);
        appendContentToFile(new File(reportDir, "report.md").toPath(), content + "\n");
    }
}
