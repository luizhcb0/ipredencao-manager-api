package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.List;

// cycleName e enrollments vêm de JOIN / detalhe; o repositório ignora no write.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BibleSchoolClass(
        Long id,
        Long cycleId,
        String cycleName,
        String name,
        String description,
        BibleSchoolClassStatusEnum status,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy,
        List<BibleSchoolEnrollment> enrollments
) {
    public BibleSchoolClass withEnrollments(List<BibleSchoolEnrollment> enrollments) {
        return new BibleSchoolClass(id, cycleId, cycleName, name, description, status,
                addedAt, updatedAt, updatedBy, enrollments);
    }
}
