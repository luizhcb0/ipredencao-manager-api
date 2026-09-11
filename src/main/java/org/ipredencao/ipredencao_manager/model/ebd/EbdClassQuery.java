package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;

// id filtra uma turma específica; os demais são filtros da listagem/busca.
// status é forçado para ACTIVE pelo service quando o chamador não é STAFF
// (visibilidade — ver EbdService.searchClasses).
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdClassQuery(
        Long id,
        String name,
        Long cycleId,
        EbdClassStatusEnum status,
        PaginationParameters pagination
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String name;
        private Long cycleId;
        private EbdClassStatusEnum status;
        private PaginationParameters pagination;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder cycleId(Long v) { this.cycleId = v; return this; }
        public Builder status(EbdClassStatusEnum v) { this.status = v; return this; }
        public Builder pagination(PaginationParameters v) { this.pagination = v; return this; }

        public EbdClassQuery build() {
            return new EbdClassQuery(id, name, cycleId, status, pagination);
        }
    }
}
