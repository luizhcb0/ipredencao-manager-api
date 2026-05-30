package org.ipredencao.ipredencao_manager.model.official_act;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OfficialActTypeEnum {
    ADMISSAO_MEMBRO_COMUNGANTE(1L,     "ADMISSAO", "Art. 16"),
    ADMISSAO_MEMBRO_NAO_COMUNGANTE(2L, "ADMISSAO", "Art. 17"),
    DEMISSAO_MEMBRO_COMUNGANTE(3L,     "DEMISSAO", "Art. 23"),
    DEMISSAO_MEMBRO_NAO_COMUNGANTE(4L, "DEMISSAO", "Art. 24");

    private final Long id;
    private final String category;
    private final String referenceArticle;

    OfficialActTypeEnum(Long id, String category, String referenceArticle) {
        this.id = id;
        this.category = category;
        this.referenceArticle = referenceArticle;
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getReferenceArticle() {
        return referenceArticle;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static OfficialActTypeEnum fromId(Long id) {
        for (OfficialActTypeEnum value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Tipo de ato oficial não encontrado para o ID: " + id);
    }
}
