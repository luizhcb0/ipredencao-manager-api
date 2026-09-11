package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

// data fica null na listagem; só findWithData (download) carrega o BYTEA.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BibleSchoolMaterial(
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
