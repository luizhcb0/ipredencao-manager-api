package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLessonStatusEnum;
import org.joda.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolLessonForm(
        String title,
        String description,
        LocalDate lessonDate,
        BibleSchoolLessonStatusEnum status
) {}
