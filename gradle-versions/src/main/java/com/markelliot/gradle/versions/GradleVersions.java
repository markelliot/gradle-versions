package com.markelliot.gradle.versions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.markelliot.gradle.versions.api.JsonSerDe;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GradleVersions {

    private static final Logger log = LoggerFactory.getLogger(GradleVersions.class);

    private GradleVersions() {}

    public enum ReleaseChannel {
        CURRENT("current"),
        RELEASE_CANDIDATE("release-candidate"),
        NIGHTLY("nightly"),
        RELEASE_NIGHTLY("release-nightly");

        private final String id;

        ReleaseChannel(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public static Optional<GradleChannelDetails> forChannel(ReleaseChannel channel) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req =
                HttpRequest.newBuilder()
                        .uri(URI.create("https://services.gradle.org/versions/" + channel.id()))
                        .GET()
                        .build();
        HttpResponse<String> resp;
        try {
            resp = client.send(req, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error(
                    "An error occurred while fetching the latest version of Gradle for channel {}",
                    channel,
                    e);
            return Optional.empty();
        }
        if (resp.statusCode() != 200) {
            log.error(
                    "An error occurred while fetching the latest version of Gradle for channel {}: {}",
                    channel,
                    resp);
            return Optional.empty();
        }
        return Optional.of(JsonSerDe.deserialize(resp.body(), GradleChannelDetails.class));
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableGradleChannelDetails.class)
    @JsonSerialize(as = ImmutableGradleChannelDetails.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public interface GradleChannelDetails {
        String version();

        @JsonProperty("downloadUrl")
        String distributionUrl();
    }
}
