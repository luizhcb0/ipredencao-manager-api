package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Um único par de endpoints (POST .../lessons/{id}/attendance cria, PATCH
// .../attendance/{id} atualiza) serve professor e aluno — ver EbdService.
// personId: só STAFF pode informar (marca/atualiza em nome de outra pessoa);
// ausente = em nome de quem está chamando. present: default true na criação.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdAttendanceForm(
        Long personId,
        Boolean present
) {}
