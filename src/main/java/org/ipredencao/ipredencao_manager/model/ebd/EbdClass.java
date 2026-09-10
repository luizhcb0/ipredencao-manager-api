package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.List;

// cycleName é derivado de JOIN. enrollments só vem preenchido no detalhe
// (findDetail), e mesmo assim só para quem tem visibilidade completa (STAFF ou
// professor da turma) — ver EbdService.getClassDetail.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdClass(
        Long id,
        Long cycleId,
        String cycleName,
        Boolean fixed,
        String name,
        String description,
        String syllabus,
        EbdClassStatusEnum status,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy,
        List<EbdEnrollment> enrollments
) {
    public EbdClass withEnrollments(List<EbdEnrollment> enrollments) {
        return new EbdClass(id, cycleId, cycleName, fixed, name, description, syllabus, status,
                addedAt, updatedAt, updatedBy, enrollments);
    }
}
