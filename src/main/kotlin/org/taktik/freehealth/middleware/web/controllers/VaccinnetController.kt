/*
 *
 * Copyright (C) 2018 Taktik SA
 *
 * This file is part of FreeHealthConnector.
 *
 * FreeHealthConnector is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation.
 *
 * FreeHealthConnector is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with FreeHealthConnector.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.taktik.freehealth.middleware.web.controllers

import be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.taktik.freehealth.middleware.dto.ehbox.AltKeystoresList
import org.taktik.freehealth.middleware.dto.ehbox.BoxInfo
import org.taktik.freehealth.middleware.dto.ehbox.DocumentMessage
import org.taktik.freehealth.middleware.dto.ehbox.ErrorMessage
import org.taktik.freehealth.middleware.dto.ehbox.Message
import org.taktik.freehealth.middleware.service.EhboxService
import org.taktik.freehealth.middleware.service.VaccinnetService
import java.util.*

@RestController
@RequestMapping("/vaccinnet")
class VaccinnetController(val vaccinnetService: VaccinnetService) {
    @GetMapping("/{patientId}", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getVaccinations(
        @RequestHeader(name = "X-FHC-keystoreId") keystoreId: UUID,
        @RequestHeader(name = "X-FHC-tokenId") tokenId: UUID,
        @RequestHeader(name = "X-FHC-passPhrase") passPhrase: String,
        @PathVariable patientId: String,
        @RequestParam softwareId: String,
        @RequestParam vaccinnetId: String,
        @RequestParam since: Long
    ) = vaccinnetService.getVaccinations(keystoreId, tokenId, passPhrase, patientId, softwareId, vaccinnetId, since)

    @DeleteMapping("/{patientId}/{vaccinationId}", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun removeVaccination(
        @RequestHeader(name = "X-FHC-keystoreId") keystoreId: UUID,
        @RequestHeader(name = "X-FHC-tokenId") tokenId: UUID,
        @RequestHeader(name = "X-FHC-passPhrase") passPhrase: String,
        @PathVariable patientId: String,
        @PathVariable vaccinationId: String,
        @RequestParam softwareId: String,
        @RequestParam vaccinnetId: String
    ) = vaccinnetService.removeVaccination(keystoreId, tokenId, passPhrase, patientId, softwareId, vaccinnetId, vaccinationId)

    @PostMapping("/{patientId}", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun addVaccinations(
        @RequestHeader(name = "X-FHC-keystoreId") keystoreId: UUID,
        @RequestHeader(name = "X-FHC-tokenId") tokenId: UUID,
        @RequestHeader(name = "X-FHC-passPhrase") passPhrase: String,
        @PathVariable patientId: String,
        @RequestParam softwareId: String,
        @RequestParam vaccinnetId: String,
        @RequestBody kmehrmessage: Kmehrmessage
    ) = vaccinnetService.addVaccinations(keystoreId, tokenId, passPhrase, patientId, softwareId, vaccinnetId, kmehrmessage)
}
