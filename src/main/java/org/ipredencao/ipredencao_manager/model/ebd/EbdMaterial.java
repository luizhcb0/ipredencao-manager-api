package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

// Só metadado — o conteúdo binário (ebd_material.file_data) nunca trafega
// aqui; ver EbdMaterialContent, usado exclusivamente pelo download.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdMaterial(
        Long id,
        Long classId,
        Long lessonId,
        String fileName,
        String contentType,
        Long fileSizeBytes,
        DateTime addedAt,
        Long updatedBy
) {}
