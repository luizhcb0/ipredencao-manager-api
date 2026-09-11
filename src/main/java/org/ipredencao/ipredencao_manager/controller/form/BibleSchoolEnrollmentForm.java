package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollmentRoleEnum;

// personId presente = STAFF em nome de alguém; ausente = o próprio chamador (sempre STUDENT).
@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolEnrollmentForm(
        Long personId,
        BibleSchoolEnrollmentRoleEnum role
) {}
