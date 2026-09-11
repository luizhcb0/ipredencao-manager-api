package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

// data (o BYTEA em si) fica null nas consultas de listagem — só o download
// (EbdMaterialRepository.findWithContent) o carrega. @JsonInclude NON_NULL
// evita expor um array vazio/gigante à toa; o controller de download nem
// serializa este record como JSON, monta um ResponseEntity<byte[]> direto.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdMaterial(
        Long id,
        Long classId,
        Long lessonId,
        String fileName,
        String contentType,
        Long fileSizeBytes,
        byte[] data,
        DateTime addedAt,
        Long updatedBy
) {}
