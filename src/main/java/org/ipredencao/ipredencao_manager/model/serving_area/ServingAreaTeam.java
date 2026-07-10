package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServingAreaTeam(
        Long id,
        Long servingAreaId,
        String name,
        String description,
        String whatsappUrl,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
