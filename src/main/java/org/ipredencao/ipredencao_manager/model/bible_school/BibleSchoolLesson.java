package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BibleSchoolLesson(
        Long id,
        Long classId,
        String title,
        String description,
        LocalDate lessonDate,
        BibleSchoolLessonStatusEnum status,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
