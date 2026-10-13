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
package org.springframework.samples.petclinic.customers.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.samples.petclinic.customers.model.PetRepository;
import org.springframework.samples.petclinic.customers.model.PetType;

import java.util.List;

/**
 * Pets metrics.
 *
 * @author Alexandre Roman
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PetsMetrics {
    private static final String ANIMALS_UNIT = "animals";
    private final MeterRegistry reg;
    private final PetRepository repo;

    @EventListener
    void registerMetrics(ApplicationReadyEvent e) {
        // Don't let a DB outage at startup (e.g. a misconfigured
        // datasource) crash an otherwise successfully-started application:
        // a failure here previously propagated out of ApplicationReadyEvent
        // handling and terminated the JVM even though the app context had
        // already come up fine.
        try {
            registerPetsMetrics();
        } catch (Exception ex) {
            log.warn("Could not register pet metrics (database may be unavailable): {}", ex.getMessage());
        }
    }

    private void registerPetsMetrics() {
        final List<PetType> petTypes = repo.findPetTypes();
        for (final PetType petType : petTypes) {
            final String petTypeName = repo.findPetTypeById(petType.getId()).get().getName();

            Gauge.builder("petclinic.pets", () -> {
                return repo.countPetsByType(petType);
            }).baseUnit("animals")
                .description("Number of pets")
                .tag("type", petTypeName)
                .register(reg);
        }
    }
}
