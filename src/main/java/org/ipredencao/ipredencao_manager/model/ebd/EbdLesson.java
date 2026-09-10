package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdLesson(
        Long id,
        Long classId,
        String title,
        String description,
        String content,
        LocalDate lessonDate,
        Integer displayOrder,
        EbdLessonStatusEnum status,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
