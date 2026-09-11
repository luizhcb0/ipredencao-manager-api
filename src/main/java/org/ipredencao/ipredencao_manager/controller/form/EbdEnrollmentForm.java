package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentRoleEnum;

// Único endpoint de matrícula (POST .../enrollments) serve os dois casos:
// personId presente = matrícula administrativa (STAFF, qualquer role); ausente
// = automatrícula (o próprio chamador, sempre role STUDENT) — ver
// EbdService.addEnrollment.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdEnrollmentForm(
        Long personId,
        EbdEnrollmentRoleEnum role
) {}
