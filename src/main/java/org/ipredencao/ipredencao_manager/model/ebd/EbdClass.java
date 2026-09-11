package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.List;

// cycleName é derivado de JOIN. enrollments só vem preenchido no detalhe
// (findDetail), e mesmo assim só para quem tem visibilidade completa (STAFF)
// — ver EbdService.getClassDetail. Toda turma pertence a um ciclo (cycleId
// obrigatório) — não existe mais distinção "turma fixa" (removida a pedido
// do time em revisão do PR).
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdClass(
        Long id,
        Long cycleId,
        String cycleName,
        String name,
        String description,
        EbdClassStatusEnum status,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy,
        List<EbdEnrollment> enrollments
) {
    public EbdClass withEnrollments(List<EbdEnrollment> enrollments) {
        return new EbdClass(id, cycleId, cycleName, name, description, status,
                addedAt, updatedAt, updatedBy, enrollments);
    }
}
