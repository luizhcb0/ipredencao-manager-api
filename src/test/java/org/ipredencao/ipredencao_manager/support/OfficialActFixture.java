package org.ipredencao.ipredencao_manager.support;

import org.ipredencao.ipredencao_manager.model.official_act.FieldType;
import org.ipredencao.ipredencao_manager.model.official_act.MetadataFieldSpec;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActCreateForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds {@link OfficialActCreateForm} for tests. Does not persist — call the service explicitly. */
public final class OfficialActFixture {

    private OfficialActFixture() {}

    public static Builder builder(OfficialActFormEnum form) {
        return new Builder(form);
    }

    /** {@code Map} with the form's {@code required} keys filled with plausible placeholders. */
    public static Map<String, Object> metadataMinimo(OfficialActFormEnum form) {
        Map<String, Object> md = new LinkedHashMap<>();
        for (MetadataFieldSpec spec : form.getMetadataFields()) {
            if (!spec.required()) continue;
            md.put(spec.key(), placeholder(spec.type(), spec.key()));
        }
        return md;
    }

    private static Object placeholder(FieldType type, String key) {
        return switch (type) {
            case STRING, TEXT -> "[" + key + "-placeholder]";
            case DATE -> "2024-01-01";
            // PERSON_REF é armazenado como objeto {id?, name}; um placeholder válido
            // precisa de `name` não-vazio (id é opcional e fica fora do fixture).
            case PERSON_REF -> Map.of("name", "[" + key + "-placeholder]");
        };
    }

    public static final class Builder {
        private final OfficialActCreateForm createForm = new OfficialActCreateForm();
        private final OfficialActFormEnum form;
        private boolean metadataSet = false;

        private Builder(OfficialActFormEnum form) {
            this.form = form;
            createForm.setOfficialActFormId(form.getId());
        }

        public Builder actDate(DateTime actDate) { createForm.setActDate(actDate); return this; }
        public Builder minuteNumber(String minuteNumber) { createForm.setMinuteNumber(minuteNumber); return this; }
        public Builder minuteDate(DateTime minuteDate) { createForm.setMinuteDate(minuteDate); return this; }
        public Builder personIds(List<Long> personIds) { createForm.setPersonIds(personIds); return this; }
        public Builder personId(Long personId) { createForm.setPersonIds(List.of(personId)); return this; }
        public Builder notes(String notes) { createForm.setNotes(notes); return this; }

        public Builder metadata(Map<String, Object> metadata) {
            createForm.setMetadata(metadata == null ? null : new HashMap<>(metadata));
            metadataSet = true;
            return this;
        }

        public Builder metadataMinimo() {
            return metadata(OfficialActFixture.metadataMinimo(form));
        }

        /** Backfill-only; HTTP controllers force these flags to {@code false}. */
        public Builder skipEffects(boolean skip) { createForm.setSkipEffects(skip); return this; }

        public Builder skipAdmissionOrderNumber(boolean skip) { createForm.setSkipAdmissionOrderNumber(skip); return this; }

        public OfficialActCreateForm build() {
            if (!metadataSet) {
                createForm.setMetadata(OfficialActFixture.metadataMinimo(form));
            }
            return createForm;
        }
    }
}
