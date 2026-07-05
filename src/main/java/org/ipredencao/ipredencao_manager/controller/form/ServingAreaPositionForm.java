package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionKindEnum;

// Usado na criação e edição de cargo. Na edição, kind é rejeitado quando o cargo
// já tem vínculos (regra no service layer); nome/sortOrder/active seguem editáveis.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaPositionForm(
        String name,
        ServingAreaPositionKindEnum kind,
        Integer sortOrder,
        Boolean active
) {}
