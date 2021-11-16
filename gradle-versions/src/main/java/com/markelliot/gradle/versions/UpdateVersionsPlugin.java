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

import com.jakewharton.nopen.annotation.Open;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

@Open
public class UpdateVersionsPlugin implements Plugin<Project> {
    @Override
    public final void apply(Project project) {
        if (project.getTasks().findByName("checkNewVersions") == null) {
            project.getTasks()
                    .create("checkNewVersions", CheckNewVersionsTask.class)
                    .setDescription(
                            "Checks for and reports on existence of newer versions of dependencies and plugins");
        } else {
            System.out.println(
                    "Project '" + project.getName() + "' already has checkNewVersions task.");
        }

        if (project.equals(project.getRootProject())) {
            project.getTasks()
                    .create("updateVersionsProps", UpdateVersionsPropsTask.class)
                    .setDescription(
                            "Uses result of checkNewVersions task to update versions.props");
            project.getTasks()
                    .create("updatePlugins", UpdatePluginsTask.class)
                    .setDescription(
                            "Uses result of checkNewVersions task to update buildscript plugin blocks");
        }
    }
}
