package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolClassQuery(
        Long id,
        List<Long> ids,
        String name,
        Long cycleId,
        BibleSchoolClassStatusEnum status,
        PaginationParameters pagination
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private List<Long> ids;
        private String name;
        private Long cycleId;
        private BibleSchoolClassStatusEnum status;
        private PaginationParameters pagination;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder ids(List<Long> v) { this.ids = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder cycleId(Long v) { this.cycleId = v; return this; }
        public Builder status(BibleSchoolClassStatusEnum v) { this.status = v; return this; }
        public Builder pagination(PaginationParameters v) { this.pagination = v; return this; }

        public BibleSchoolClassQuery build() {
            return new BibleSchoolClassQuery(id, ids, name, cycleId, status, pagination);
        }
    }
}
