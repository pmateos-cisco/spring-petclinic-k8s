/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.api.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodically generates random test data (owners, pets, vets) against the
 * backend services when enabled via the toggle button in the UI.
 * <p>
 * This is also a diagnostic tool: when {@code customers-service} is deployed
 * as multiple versions behind the same load-balanced Kubernetes
 * {@code Service}, every response carries an {@code X-App-Version} header
 * (see {@code VersionHeaderFilter} in customers-service). Success/failure
 * counts are tracked per version so you can watch one version succeed while
 * another fails, live.
 */
@Service
@Slf4j
public class TestDataGeneratorService {

    private static final long INTERVAL_MS = 5 * 60 * 1000L; // 5 minutes

    private static final String[] FIRST_NAMES = {
        "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Jamie", "Avery", "Quinn", "Drew"
    };
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Martinez", "Wilson"
    };
    private static final String[] CITIES = {
        "Madison", "Sun Prairie", "McFarland", "Windsor", "Monona", "Waunakee"
    };
    private static final String[] STREETS = {
        "Main St.", "Oak Blvd.", "Liberty St.", "Lake St.", "Commerce St.", "Fair Way"
    };
    private static final String[] PET_NAMES = {
        "Rex", "Bella", "Charlie", "Luna", "Milo", "Coco", "Oscar", "Daisy"
    };
    private static final int[] PET_TYPE_IDS = {1, 2, 3, 4, 5, 6};
    private static final String[] SPECIALTIES = {"radiology", "surgery", "dentistry"};

    private final WebClient.Builder webClientBuilder;
    private final String customersUri;
    private final String vetsUri;

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicLong totalRuns = new AtomicLong();
    private final Map<String, VersionStats> versionStats = new ConcurrentHashMap<>();
    private volatile Instant lastRunAt;
    private volatile String lastError;

    public TestDataGeneratorService(WebClient.Builder webClientBuilder,
                                     @Value("${app.customers.uri}") String customersUri,
                                     @Value("${app.vets.uri}") String vetsUri) {
        this.webClientBuilder = webClientBuilder;
        this.customersUri = customersUri;
        this.vetsUri = vetsUri;
    }

    public Map<String, Object> toggle() {
        final boolean newState = !enabled.get();
        enabled.set(newState);
        log.info("Test data generator {}", newState ? "ENABLED" : "DISABLED");
        return status();
    }

    public Map<String, Object> status() {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled.get());
        result.put("intervalSeconds", INTERVAL_MS / 1000);
        result.put("totalRuns", totalRuns.get());
        result.put("lastRunAt", lastRunAt == null ? null : lastRunAt.toString());
        result.put("lastError", lastError);

        final Map<String, Object> versions = new LinkedHashMap<>();
        versionStats.forEach((version, stats) -> {
            final Map<String, Object> counts = new LinkedHashMap<>();
            counts.put("success", stats.success.get());
            counts.put("failure", stats.failure.get());
            versions.put(version, counts);
        });
        result.put("versions", versions);
        return result;
    }

    @Scheduled(initialDelay = 30_000L, fixedDelay = INTERVAL_MS)
    public void generate() {
        if (!enabled.get()) {
            return;
        }
        totalRuns.incrementAndGet();
        lastRunAt = Instant.now();
        try {
            createOwnerWithPet();
            lastError = null;
        } catch (Exception ex) {
            lastError = ex.getMessage();
            log.warn("Test data generation (owner/pet) failed: {}", ex.getMessage());
        }
        try {
            createVet();
        } catch (Exception ex) {
            log.warn("Test data generation (vet) failed: {}", ex.getMessage());
        }
    }

    private void createOwnerWithPet() {
        final Map<String, Object> ownerPayload = new LinkedHashMap<>();
        ownerPayload.put("firstName", randomOf(FIRST_NAMES));
        ownerPayload.put("lastName", randomOf(LAST_NAMES));
        ownerPayload.put("address", (1 + ThreadLocalRandom.current().nextInt(9999)) + " " + randomOf(STREETS));
        ownerPayload.put("city", randomOf(CITIES));
        ownerPayload.put("telephone", randomPhone());

        final int ownerId;
        try {
            final ResponseEntity<Map> response = webClientBuilder.build().post()
                .uri(customersUri + "/owners")
                .bodyValue(ownerPayload)
                .retrieve()
                .toEntity(Map.class)
                .block();
            final String version = versionOf(response.getHeaders());
            recordSuccess(version);
            ownerId = ((Number) response.getBody().get("id")).intValue();
            log.info("Test data generation (owner) succeeded against version={}, ownerId={}", version, ownerId);
        } catch (WebClientResponseException ex) {
            recordFailure(versionOf(ex.getHeaders()));
            throw ex;
        }

        final Map<String, Object> petPayload = new LinkedHashMap<>();
        petPayload.put("name", randomOf(PET_NAMES));
        petPayload.put("birthDate",
            LocalDate.now().minusYears(1 + ThreadLocalRandom.current().nextInt(8)).toString());
        petPayload.put("typeId", PET_TYPE_IDS[ThreadLocalRandom.current().nextInt(PET_TYPE_IDS.length)]);

        try {
            final ResponseEntity<Map> petResponse = webClientBuilder.build().post()
                .uri(customersUri + "/owners/{ownerId}/pets", ownerId)
                .bodyValue(petPayload)
                .retrieve()
                .toEntity(Map.class)
                .block();
            final String version = versionOf(petResponse.getHeaders());
            recordSuccess(version);
            log.info("Test data generation (pet) succeeded against version={}, ownerId={}", version, ownerId);
        } catch (WebClientResponseException ex) {
            recordFailure(versionOf(ex.getHeaders()));
            throw ex;
        }
    }

    private void createVet() {
        final Map<String, Object> vetPayload = new LinkedHashMap<>();
        vetPayload.put("firstName", randomOf(FIRST_NAMES));
        vetPayload.put("lastName", randomOf(LAST_NAMES));
        final List<String> specialties = Collections.singletonList(randomOf(SPECIALTIES));
        vetPayload.put("specialties", specialties);

        webClientBuilder.build().post()
            .uri(vetsUri + "/vets")
            .bodyValue(vetPayload)
            .retrieve()
            .toBodilessEntity()
            .block();
        log.info("Test data generation (vet) succeeded");
    }

    private void recordSuccess(String version) {
        versionStats.computeIfAbsent(version, v -> new VersionStats()).success.incrementAndGet();
    }

    private void recordFailure(String version) {
        versionStats.computeIfAbsent(version, v -> new VersionStats()).failure.incrementAndGet();
    }

    private static String versionOf(HttpHeaders headers) {
        final String version = headers.getFirst("X-App-Version");
        return version == null ? "unknown" : version;
    }

    private static String randomOf(String[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private static String randomPhone() {
        final StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    private static final class VersionStats {
        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger failure = new AtomicInteger();
    }
}
