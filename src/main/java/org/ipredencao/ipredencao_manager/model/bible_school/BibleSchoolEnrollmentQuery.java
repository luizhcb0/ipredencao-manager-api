package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolEnrollmentQuery(
        Long id,
        Long classId,
        Long personId,
        BibleSchoolEnrollmentRoleEnum role
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long classId;
        private Long personId;
        private BibleSchoolEnrollmentRoleEnum role;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder classId(Long v) { this.classId = v; return this; }
        public Builder personId(Long v) { this.personId = v; return this; }
        public Builder role(BibleSchoolEnrollmentRoleEnum v) { this.role = v; return this; }

        public BibleSchoolEnrollmentQuery build() {
            return new BibleSchoolEnrollmentQuery(id, classId, personId, role);
        }
    }
}
