package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

// className/personName vêm de JOIN; o repositório ignora no write.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BibleSchoolEnrollment(
        Long id,
        Long classId,
        String className,
        Long personId,
        String personName,
        BibleSchoolEnrollmentRoleEnum role,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
