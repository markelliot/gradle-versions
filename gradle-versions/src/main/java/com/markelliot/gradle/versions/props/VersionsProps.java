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

package com.markelliot.gradle.versions.props;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.immutables.value.Value;

public final class VersionsProps {
    private static final Pattern COMMENT_OR_EMPTY = Pattern.compile("(\\s*#.*|\\s*)");
    private static final Pattern VERSION_LINE =
            Pattern.compile(
                    "(?<identifier>[^#\\s:]+:[^#\\s]+)\\s*=\\s*(?<version>[^#\\s]+)(\\s*#\\s*(?<comment>.+))?");

    private final List<Line> lines;
    private final FuzzyPatternResolver resolver;

    @Value.Immutable
    public interface UpdatedLine {
        String dependency();

        String oldVersion();

        String newVersion();
    }

    private VersionsProps(List<Line> lines, FuzzyPatternResolver resolver) {
        this.lines = lines;
        this.resolver = resolver;
    }

    public Optional<UpdatedLine> update(String identifier, String version) {
        Optional<String> maybeMatch = resolver.patternFor(identifier);
        if (maybeMatch.isEmpty()) {
            System.out.println("No matching pattern for '" + identifier + "'");
            return Optional.empty();
        }

        String bestMatch = maybeMatch.get();
        int i = 0;
        for (Line line : lines) {
            if (line.type() == LineType.Version) {
                VersionLine versionLine = (VersionLine) line;
                if (versionLine.identifier().equals(bestMatch)) {
                    if (versionLine.version().equals(version)) {
                        // found best match but version is already what we expect
                        return Optional.empty();
                    }
                    System.out.printf("Setting %s = %s\n", bestMatch, version);
                    lines.set(
                            i,
                            ImmutableVersionLine.builder()
                                    .from(versionLine)
                                    .version(version)
                                    .build());
                    return Optional.of(
                            ImmutableUpdatedLine.builder()
                                    .dependency(bestMatch)
                                    .oldVersion(versionLine.version())
                                    .newVersion(version)
                                    .build());
                }
            }
            i++;
        }

        return Optional.empty();
    }

    public String writeToString() {
        return lines.stream().map(Line::emit).collect(Collectors.joining("\n")) + "\n";
    }

    public static VersionsProps from(File file) {
        return from(file.toPath());
    }

    public static VersionsProps from(Path path) {
        try {
            return from(Files.readAllLines(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void to(File file) {
        to(file.toPath());
    }

    public void to(Path path) {
        try {
            Files.writeString(
                    path,
                    writeToString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static VersionsProps from(Iterable<String> in) {
        List<Line> parsedLines = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        for (String line : in) {
            if (isCommentOrEmpty(line)) {
                parsedLines.add(ImmutableNonVersionLine.builder().content(line).build());
            } else {
                Matcher matcher = VERSION_LINE.matcher(line);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid version line '" + line + "'");
                }
                String identifier = matcher.group("identifier");
                String version = matcher.group("version");
                parsedLines.add(
                        ImmutableVersionLine.builder()
                                .identifier(identifier)
                                .version(version)
                                .comment(Optional.ofNullable(matcher.group("comment")))
                                .build());
                identifiers.add(identifier);
            }
        }
        return new VersionsProps(parsedLines, new FuzzyPatternResolver(identifiers));
    }

    private static boolean isCommentOrEmpty(String line) {
        return COMMENT_OR_EMPTY.matcher(line).matches();
    }

    enum LineType {
        NonVersion,
        Version
    }

    interface Line {
        String emit();

        LineType type();
    }

    @Value.Immutable
    interface NonVersionLine extends Line {
        String content();

        @Value.Derived
        @Override
        default String emit() {
            return content();
        }

        @Value.Default
        @Override
        default LineType type() {
            return LineType.NonVersion;
        }
    }

    @Value.Immutable
    interface VersionLine extends Line {
        String identifier();

        String version();

        Optional<String> comment();

        @Value.Derived
        @Override
        default String emit() {
            return identifier() + " = " + version() + comment().map(c -> " # " + c).orElse("");
        }

        @Value.Default
        @Override
        default LineType type() {
            return LineType.Version;
        }
    }
}
