package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BibleSchoolCycle(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
