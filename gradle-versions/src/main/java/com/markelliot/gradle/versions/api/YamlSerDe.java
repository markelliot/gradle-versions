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

package com.markelliot.gradle.versions.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YamlSerDe {
    private static final ObjectMapper mapper =
            new ObjectMapper(
                            new YAMLFactory()
                                    .enable(Feature.MINIMIZE_QUOTES)
                                    .disable(Feature.WRITE_DOC_START_MARKER))
                    .registerModule(new GuavaModule())
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private YamlSerDe() {}

    public static <T> String serialize(T obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> void serialize(File file, T obj) {
        try {
            mapper.writeValue(file, obj);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T deserialize(String val, Class<T> type) {
        try {
            return mapper.readValue(val, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T deserialize(Path path, Class<T> type) {
        try {
            return deserialize(Files.readString(path), type);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
