package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassStatusEnum;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolClassForm(
        Long cycleId,
        String name,
        String description,
        BibleSchoolClassStatusEnum status
) {}
