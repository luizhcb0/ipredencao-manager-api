package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

// personId/personName vêm de JOIN via matrícula; o repositório ignora no write.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BibleSchoolAttendance(
        Long id,
        Long lessonId,
        Long enrollmentId,
        Long personId,
        String personName,
        Boolean present,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
