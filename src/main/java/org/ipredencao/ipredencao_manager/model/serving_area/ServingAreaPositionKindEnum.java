package org.ipredencao.ipredencao_manager.model.serving_area;

// Nível hierárquico do cargo dentro da área de serviço. Enum nativo do Postgres
// (serving_area_position_kind); na wire é string plain ("SUPERVISION", ...).
public enum ServingAreaPositionKindEnum {
    SUPERVISION,
    COORDINATION,
    MEMBERSHIP
}
