package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// personId: só STAFF pode informar (marca em nome de outra pessoa); ausente = quem chama.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolAttendanceForm(
        Long personId,
        Boolean present
) {}
