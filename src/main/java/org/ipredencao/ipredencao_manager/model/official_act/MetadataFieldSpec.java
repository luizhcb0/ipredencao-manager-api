package org.ipredencao.ipredencao_manager.model.official_act;

public record MetadataFieldSpec(
        String key,
        FieldType type,
        boolean required,
        String helperText
) {
    public static MetadataFieldSpec req(String key, FieldType type, String helperText) {
        return new MetadataFieldSpec(key, type, true, helperText);
    }

    public static MetadataFieldSpec opt(String key, FieldType type, String helperText) {
        return new MetadataFieldSpec(key, type, false, helperText);
    }
}
