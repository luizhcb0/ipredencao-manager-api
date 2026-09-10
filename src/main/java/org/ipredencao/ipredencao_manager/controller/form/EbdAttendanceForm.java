package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Só para retificação (PATCH .../attendance/{id}) por STAFF/professor. A
// automarcação (POST .../lessons/{id}/attendance/me) não recebe body: é sempre
// present=true, self-reported.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdAttendanceForm(
        Boolean present
) {}
