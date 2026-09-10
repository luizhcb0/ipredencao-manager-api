package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLessonStatusEnum;
import org.joda.time.LocalDate;

// displayOrder nulo em criação = anexa ao fim (ver EbdService.createLesson).
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdLessonForm(
        String title,
        String description,
        String content,
        LocalDate lessonDate,
        Integer displayOrder,
        EbdLessonStatusEnum status
) {}
