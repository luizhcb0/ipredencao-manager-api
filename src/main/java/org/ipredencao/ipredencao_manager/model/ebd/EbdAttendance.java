package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

// personId/personName são derivados de JOIN (via ebd_enrollment).
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdAttendance(
        Long id,
        Long lessonId,
        Long enrollmentId,
        Long personId,
        String personName,
        Boolean present,
        Boolean selfReported,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
