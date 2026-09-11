package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

// className/personName são derivados de JOIN. Sem startDate/endDate — "desde
// quando"/"até quando" não importam mais pro negócio (ver comentário em V015);
// quando o vínculo é removido, ebd_enrollment_history.deletedAt registra isso.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EbdEnrollment(
        Long id,
        Long classId,
        String className,
        Long personId,
        String personName,
        EbdEnrollmentRoleEnum role,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy
) {}
