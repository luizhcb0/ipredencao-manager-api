package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdEnrollmentQuery(
        Long id,
        Long classId,
        Long personId,
        EbdEnrollmentRoleEnum role
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long classId;
        private Long personId;
        private EbdEnrollmentRoleEnum role;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder classId(Long v) { this.classId = v; return this; }
        public Builder personId(Long v) { this.personId = v; return this; }
        public Builder role(EbdEnrollmentRoleEnum v) { this.role = v; return this; }

        public EbdEnrollmentQuery build() {
            return new EbdEnrollmentQuery(id, classId, personId, role);
        }
    }
}
