package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Usado na criação e edição de área. active é opcional: no create, nulo vira
// true; no update, nulo mantém o valor atual (tratado no service layer).
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaForm(
        String name,
        String description,
        String whatsappUrl,
        String coordinationWhatsappUrl,
        Boolean active
) {}
