package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

// className/personName são derivados de JOIN.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdEnrollment(
        Long id,
        Long classId,
        String className,
        Long personId,
        String personName,
        EbdEnrollmentRoleEnum role,
        LocalDate startDate,
        LocalDate endDate,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
