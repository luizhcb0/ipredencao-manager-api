package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassStatusEnum;

// cycleId é obrigatório (toda turma pertence a um ciclo). Ementa não entra
// aqui — é um arquivo, enviado via POST /classes/{id}/syllabus (multipart),
// não pelo JSON de criação/edição da turma.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdClassForm(
        Long cycleId,
        String name,
        String description,
        EbdClassStatusEnum status
) {}
