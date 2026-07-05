package org.ipredencao.ipredencao_manager.support;

import org.ipredencao.ipredencao_manager.controller.form.ServingAreaForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaMemberForm;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingArea;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMember;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionKindEnum;
import org.ipredencao.ipredencao_manager.service.ServingAreaService;
import org.joda.time.LocalDate;

/** Builds serving areas / members for integration tests via the service. */
public final class ServingAreaFixture {

    private ServingAreaFixture() {}

    // Cria uma área comum (nasce com os 3 cargos padrão).
    public static ServingArea area(ServingAreaService service, String name) {
        return service.create(new ServingAreaForm(name, "Área de teste", null, null, null));
    }

    // Id do cargo padrão de um dado kind na área recém-criada.
    public static Long positionId(ServingArea area, ServingAreaPositionKindEnum kind) {
        return area.positions().stream()
                .filter(p -> p.kind() == kind)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cargo " + kind + " não encontrado na área"))
                .id();
    }

    public static ServingAreaMember addMember(ServingAreaService service, Long areaId, Long personId,
                                              Long positionId, Long teamId, LocalDate start) {
        return service.addMember(areaId, new ServingAreaMemberForm(personId, positionId, teamId, start));
    }
}
