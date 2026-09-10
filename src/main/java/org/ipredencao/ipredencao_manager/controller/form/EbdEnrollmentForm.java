package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentRoleEnum;
import org.joda.time.LocalDate;

// Usado só na matrícula administrativa (STAFF/professor) — POST .../enrollments.
// A automatrícula (POST .../enrollments/me) não recebe body: personId/role vêm
// do usuário logado (role fixo STUDENT).
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdEnrollmentForm(
        Long personId,
        EbdEnrollmentRoleEnum role,
        LocalDate startDate
) {}
