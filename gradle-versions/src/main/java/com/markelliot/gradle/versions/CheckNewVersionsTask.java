/*
 * (c) Copyright 2021 Mark Elliot. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markelliot.gradle.versions;

import com.google.common.base.Preconditions;
import com.jakewharton.nopen.annotation.Open;
import com.markelliot.gradle.versions.api.DependencyUpdateRec;
import com.markelliot.gradle.versions.api.ImmutableDependencyUpdateRec;
import com.markelliot.gradle.versions.api.ImmutableUpdateReport;
import com.markelliot.gradle.versions.api.SerDe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.ComponentSelection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.TaskAction;

@Open
public class CheckNewVersionsTask extends DefaultTask {
    public static final String REPORT_DIRNAME = "com.markelliot.versions";
    public static final String REPORT_YML = "report.yml";

    // TODO(markelliot): make these configurable
    private static final Set<String> DISALLOWED_QUALIFIERS = Set.of("-alpha", "-beta", "-rc");

    @TaskAction
    public final void taskAction() throws IOException {
        Map<String, Set<DependencyUpdateRec>> updatesByConfig =
                getProject().getConfigurations().stream()
                        // make safe for use with gradle-consistent-versions
                        .filter(config -> !config.getName().startsWith("consistentVersions"))
                        .filter(config -> !config.getName().equals("unifiedClasspath"))
                        .collect(
                                Collectors.toMap(
                                        Configuration::getName, this::getRecsForConfiguration));

        Set<DependencyUpdateRec> dependencyUpdates =
                updatesByConfig.values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
        if (!dependencyUpdates.isEmpty()) {
            System.out.println(
                    "Dependency upgrades available for project '" + getProject().getName() + "'");
            dependencyUpdates.forEach(rec -> System.out.println("   - " + render(rec)));
        }

        // Suggest upgrades for plugins
        Configuration pluginConfiguration =
                getProject().getBuildscript().getConfigurations().getByName("classpath");
        Set<DependencyUpdateRec> pluginUpdates = getRecsForConfiguration(pluginConfiguration);
        if (!pluginUpdates.isEmpty()) {
            System.out.println(
                    "Plugin upgrades available for project '" + getProject().getName() + "'");
            pluginUpdates.forEach(upgrade -> System.out.println("- " + render(upgrade)));
        }

        // emit report
        File reportDir = new File(getProject().getBuildDir(), REPORT_DIRNAME);
        Preconditions.checkState(
                reportDir.exists() || reportDir.mkdirs(), "unable to make reportDir");
        File reportFile = new File(reportDir, REPORT_YML);
        String reportContent =
                SerDe.serialize(
                        ImmutableUpdateReport.builder()
                                .project(getProject().getPath())
                                .addAllDependencyUpdates(dependencyUpdates)
                                .addAllPluginUpdates(pluginUpdates)
                                .build());
        Files.writeString(
                reportFile.toPath(),
                reportContent,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String render(DependencyUpdateRec detail) {
        return detail.group()
                + ":"
                + detail.name()
                + ":{"
                + detail.currentVersion()
                + " -> "
                + detail.latestVersion()
                + "}";
    }

    private Set<DependencyUpdateRec> getRecsForConfiguration(Configuration config) {
        Map<String, ResolvedDependency> currentVersions = getCurrentDependencyVersions(config);
        Map<String, ResolvedDependency> latestVersions =
                getLatestDependencyVersions(config, currentVersions);

        return currentVersions.entrySet().stream()
                .flatMap(
                        entry -> {
                            if (!latestVersions.containsKey(entry.getKey())) {
                                // possible to reach here because of project dependencies or
                                // unresolvable new versions
                                return Stream.empty();
                            }
                            String currentVersion = entry.getValue().getModuleVersion();
                            String latestVersion =
                                    latestVersions.get(entry.getKey()).getModuleVersion();
                            if (currentVersion.equals(latestVersion)) {
                                return Stream.empty();
                            }
                            return Stream.of(
                                    ImmutableDependencyUpdateRec.builder()
                                            .group(entry.getValue().getModuleGroup())
                                            .name(entry.getValue().getModuleName())
                                            .currentVersion(currentVersion)
                                            .latestVersion(latestVersion)
                                            .build());
                        })
                .collect(Collectors.toSet());
    }

    private Map<String, ResolvedDependency> getCurrentDependencyVersions(Configuration config) {
        Configuration resolvableOriginal = getResolvableCopy(config);
        return getResolvedVersions(resolvableOriginal);
    }

    private Map<String, ResolvedDependency> getLatestDependencyVersions(
            Configuration config, Map<String, ResolvedDependency> currentVersions) {
        Configuration resolvableLatest = getResolvableCopy(config);

        resolvableLatest.resolutionStrategy(
                strat -> strat.componentSelection(rules -> rules.all(this::selectOnlyRelease)));

        Set<Dependency> latestDepsForConfig =
                currentVersions.keySet().stream()
                        .map(key -> getProject().getDependencies().create(key + ":+"))
                        .collect(Collectors.toSet());
        resolvableLatest.getDependencies().clear();
        resolvableLatest.getDependencies().addAll(latestDepsForConfig);
        // TODO(markelliot): we may want to find a way to tweak the resolution strategy so that
        //  forced module overrides still get a recommended upgrade
        return getResolvedVersions(resolvableLatest);
    }

    private void selectOnlyRelease(ComponentSelection sel) {
        String version = sel.getCandidate().getVersion();
        if (containsDisallowedQualifier(version)) {
            sel.reject("Component is 'alpha' or 'beta' qualified (version=" + version + ")");
        }
        String status = sel.getMetadata() != null ? sel.getMetadata().getStatus() : null;
        if (status != null && !status.equals("release")) {
            sel.reject("Component status '" + status + "' was not 'release'");
        }
    }

    private boolean containsDisallowedQualifier(String version) {
        return DISALLOWED_QUALIFIERS.stream().anyMatch(version::contains);
    }

    private Configuration getResolvableCopy(Configuration config) {
        Configuration resolvableConfig = config.copyRecursive().setTransitive(false);
        resolvableConfig.setCanBeResolved(true);
        return resolvableConfig;
    }

    private Map<String, ResolvedDependency> getResolvedVersions(Configuration config) {
        LenientConfiguration lenientConfig =
                config.getResolvedConfiguration().getLenientConfiguration();
        Set<ResolvedDependency> moduleDeps =
                lenientConfig.getFirstLevelModuleDependencies(Specs.SATISFIES_ALL);
        Map<String, ResolvedDependency> resolvedDeps = new HashMap<>();
        moduleDeps.forEach(
                dep -> resolvedDeps.put(dep.getModuleGroup() + ":" + dep.getModuleName(), dep));
        return resolvedDeps;
    }
}
