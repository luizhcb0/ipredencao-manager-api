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
// Ementa é um arquivo (BYTEA), não mais texto — syllabusFileName/ContentType/
// FileSizeBytes são só metadado (nulos quando a turma não tem ementa
// enviada); o conteúdo binário nunca trafega aqui, só via download
// (EbdService.downloadSyllabus / EbdClassRepository.findSyllabusContent).
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdClass(
        Long id,
        Long cycleId,
        String cycleName,
        String name,
        String description,
        String syllabusFileName,
        String syllabusContentType,
        Long syllabusFileSizeBytes,
        EbdClassStatusEnum status,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy,
        List<EbdEnrollment> enrollments
) {
    public EbdClass withEnrollments(List<EbdEnrollment> enrollments) {
        return new EbdClass(id, cycleId, cycleName, name, description, syllabusFileName, syllabusContentType,
                syllabusFileSizeBytes, status, addedAt, updatedAt, updatedBy, enrollments);
    }
}
