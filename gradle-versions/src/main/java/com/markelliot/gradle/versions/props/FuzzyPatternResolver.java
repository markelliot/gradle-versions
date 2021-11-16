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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Adapted from {@code nebula.dependency-recommender}. */
public final class FuzzyPatternResolver {
    private final Set<String> identifiers;

    public FuzzyPatternResolver(Set<String> identifiers) {
        this.identifiers = identifiers;
    }

    private Set<String> exactMatches() {
        return identifiers.stream()
                .filter(name -> !name.contains("*"))
                .collect(ImmutableSet.toImmutableSet());
    }

    private List<Glob> globs() {
        List<Glob> cache = new ArrayList<>();
        for (String name : identifiers) {
            if (name.contains("*")) {
                cache.add(Glob.compile(name));
            }
        }
        // Sorting in order to prefer more specific globs (with more non-* characters), see the Glob
        // class below.
        // The more specific globs will end up at the beginning of the array.
        Collections.sort(cache);
        return cache;
    }

    public Optional<String> patternFor(String key) {
        // Always prefer exact matches (which should be handled separately).
        if (exactMatches().contains(key)) {
            return Optional.of(key);
        }

        for (Glob glob : globs()) {
            if (glob.matches(key)) {
                return Optional.of(glob.rawPattern);
            }
        }

        return Optional.empty();
    }

    private static final class Glob implements Comparable<Glob> {
        private final Pattern pattern;
        private final String rawPattern;
        private final int weight;

        Glob(Pattern pattern, String rawPattern, int weight) {
            this.pattern = pattern;
            this.rawPattern = rawPattern;
            this.weight = weight;
        }

        private static Glob compile(String glob) {
            StringBuilder patternBuilder = new StringBuilder();
            boolean first = true;
            int weight = 0;

            for (String token : glob.split("\\*", -1)) {
                if (first) {
                    first = false;
                } else {
                    patternBuilder.append(".*?");
                }

                weight += token.length();
                patternBuilder.append(Pattern.quote(token));
            }

            Pattern pattern = Pattern.compile(patternBuilder.toString());

            return new Glob(pattern, glob, weight);
        }

        String getRawPattern() {
            return rawPattern;
        }

        boolean matches(String key) {
            return pattern.matcher(key).matches();
        }

        @Override
        public int compareTo(Glob other) {
            return Integer.compare(other.weight, weight);
        }
    }
}
