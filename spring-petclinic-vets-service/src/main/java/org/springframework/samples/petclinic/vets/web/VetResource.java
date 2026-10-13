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
package org.springframework.samples.petclinic.vets.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.samples.petclinic.vets.model.Specialty;
import org.springframework.samples.petclinic.vets.model.SpecialtyRepository;
import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Maciej Szarlinski
 */
@RequestMapping("/vets")
@RestController
@RequiredArgsConstructor
class VetResource {

    private final VetRepository vetRepository;

    private final SpecialtyRepository specialtyRepository;

    @GetMapping
    public List<Vet> showResourcesVetList() {
        return vetRepository.findAll();
    }

    /**
     * Create a new Vet. Specialty names that don't already exist are created
     * on the fly.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vet createVet(@Valid @RequestBody VetRequest request) {
        final Vet vet = new Vet();
        vet.setFirstName(request.getFirstName());
        vet.setLastName(request.getLastName());

        if (request.getSpecialties() != null) {
            for (String name : request.getSpecialties()) {
                final Optional<Specialty> existing = specialtyRepository.findByName(name);
                final Specialty specialty;
                if (existing.isPresent()) {
                    specialty = existing.get();
                } else {
                    final Specialty newSpecialty = new Specialty();
                    newSpecialty.setName(name);
                    specialty = specialtyRepository.save(newSpecialty);
                }
                vet.addSpecialty(specialty);
            }
        }

        return vetRepository.save(vet);
    }
}
