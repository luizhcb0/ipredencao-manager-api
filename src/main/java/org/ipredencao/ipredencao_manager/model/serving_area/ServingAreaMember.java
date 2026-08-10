package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

// servingAreaName / person* / positionName / kind / teamName são derivados de JOIN.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServingAreaMember(
        Long id,
        Long servingAreaId,
        Long personId,
        Long positionId,
        Long teamId,
        LocalDate startDate,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy,
        String servingAreaName,
        String personName,
        Long personCategoryId,
        String personCategoryName,
        String positionName,
        ServingAreaPositionKindEnum kind,
        String teamName
) {}
