package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLessonStatusEnum;
import org.joda.time.LocalDate;

// lessonDate é obrigatória — é o único critério de ordenação das aulas.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdLessonForm(
        String title,
        String description,
        LocalDate lessonDate,
        EbdLessonStatusEnum status
) {}
