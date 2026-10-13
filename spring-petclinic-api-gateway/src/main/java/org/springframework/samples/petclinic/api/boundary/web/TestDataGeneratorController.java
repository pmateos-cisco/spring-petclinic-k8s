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
package org.springframework.samples.petclinic.api.boundary.web;

import lombok.RequiredArgsConstructor;
import org.springframework.samples.petclinic.api.application.TestDataGeneratorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controls the background test data generator (owners/pets/vets) used to
 * demo rolling/canary deployments.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gateway/test-data-generator")
public class TestDataGeneratorController {

    private final TestDataGeneratorService testDataGeneratorService;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return testDataGeneratorService.status();
    }

    @PostMapping("/toggle")
    public Map<String, Object> toggle() {
        return testDataGeneratorService.toggle();
    }
}
