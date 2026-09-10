package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassStatusEnum;

// fixed/cycleId são estruturais: só o STAFF pode defini-los ou alterá-los (o
// professor da turma pode editar os demais campos) — ver EbdService.updateClass.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdClassForm(
        Long cycleId,
        Boolean fixed,
        String name,
        String description,
        String syllabus,
        EbdClassStatusEnum status
) {}
