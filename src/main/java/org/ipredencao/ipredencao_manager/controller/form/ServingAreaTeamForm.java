package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Usado na criação e edição de equipe.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaTeamForm(
        String name,
        String description,
        String whatsappUrl
) {}
