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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Splitter;
import com.markelliot.gradle.versions.props.VersionsProps;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class VersionsPropsTests {
    @Test
    public void testMutation() {
        VersionsProps props =
                VersionsProps.from(
                        List.of(
                                "# comment",
                                "   ", // non-comment non-version line
                                "", // empty
                                "org.slf4j:slf4j-api = 1.7.15 # bar",
                                "com.foo.bar:qux = 1.2",
                                "org.slf4j:* = 1.7.12"));

        props.update("org.slf4j:none", "1.7.24");
        props.update("org.slf4j:slf4j-api", "1.7.26");

        List<String> lines = Splitter.on('\n').splitToList(props.writeToString());
        assertThat(lines)
                .contains("org.slf4j:* = 1.7.24")
                .contains("org.slf4j:slf4j-api = 1.7.26 # bar");
    }

    @Test
    public void testNoUpdateForSameValue() {
        VersionsProps props = VersionsProps.from(List.of("com.foo.bar:qux = 1.2"));

        Optional<VersionsProps.UpdatedLine> updates = props.update("com.foo.bar:qux", "1.2");
        assertThat(updates).isEmpty();
    }
}
