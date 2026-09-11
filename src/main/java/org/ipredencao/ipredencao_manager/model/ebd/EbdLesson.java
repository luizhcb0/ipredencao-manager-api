package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

// Sem content nem displayOrder: descrição + material bastam pro conteúdo, e a
// ordem de exibição é sempre por lessonDate (por isso ela é obrigatória).
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdLesson(
        Long id,
        Long classId,
        String title,
        String description,
        LocalDate lessonDate,
        EbdLessonStatusEnum status,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
