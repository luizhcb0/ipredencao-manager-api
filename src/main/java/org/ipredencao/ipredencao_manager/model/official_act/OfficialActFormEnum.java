package org.ipredencao.ipredencao_manager.model.official_act;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;

import static org.ipredencao.ipredencao_manager.model.official_act.MetadataFieldSpec.opt;
import static org.ipredencao.ipredencao_manager.model.official_act.MetadataFieldSpec.req;
import static org.ipredencao.ipredencao_manager.model.official_act.OfficialActTypeEnum.*;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OfficialActFormEnum {

    // Admissão de membro comungante (Art. 16)
    ADM_MC_PROFISSAO_FE(1L, ADMISSAO_MEMBRO_COMUNGANTE, "Art. 16, a", List.of(
            req("celebrant", FieldType.PERSON_REF, "Celebrante")
    )),
    ADM_MC_PROFISSAO_FE_E_BATISMO(2L, ADMISSAO_MEMBRO_COMUNGANTE, "Art. 16, b", List.of(
            req("celebrant", FieldType.PERSON_REF, "Celebrante")
    )),
    ADM_MC_CARTA_TRANSFERENCIA(3L, ADMISSAO_MEMBRO_COMUNGANTE, "Art. 16, c", List.of(
            req("originChurch",      FieldType.STRING, "Nome + cidade/UF da igreja de procedência"),
            opt("originPresbytery",  FieldType.STRING, "Presbitério de origem"),
            opt("letterNumber",      FieldType.STRING, "Referência da carta")
    )),
    ADM_MC_JURISDICAO_A_PEDIDO(4L, ADMISSAO_MEMBRO_COMUNGANTE, "Art. 16, d", List.of(
            req("originChurch",      FieldType.STRING, "Igreja de origem"),
            opt("originPresbytery",  FieldType.STRING, "Presbitério de origem"),
            opt("reason",            FieldType.TEXT,   "Motivo do pedido")
    )),
    ADM_MC_JURISDICAO_EX_OFFICIO(5L, ADMISSAO_MEMBRO_COMUNGANTE, "Art. 16, e", List.of(
            req("originChurch",      FieldType.STRING, "Igreja de origem"),
            opt("originPresbytery",  FieldType.STRING, "Presbitério de origem"),
            opt("resolutionNumber",  FieldType.STRING, "Número da resolução")
    )),
    ADM_MC_RESTAURACAO(6L, ADMISSAO_MEMBRO_COMUNGANTE, "Art. 16, f", List.of(
            opt("reason", FieldType.TEXT, "Motivo / contexto da restauração")
    )),
    ADM_MC_DESIGNACAO_PRESBITERIO(7L, ADMISSAO_MEMBRO_COMUNGANTE, "Art. 16, g", List.of(
            req("originPresbytery",  FieldType.STRING, "Presbitério designante"),
            opt("resolutionNumber",  FieldType.STRING, "Número da resolução"),
            opt("reason",            FieldType.TEXT,   "Motivo")
    )),

    // Admissão de membro não comungante (Art. 17)
    ADM_MNC_BATISMO_INFANCIA(8L, ADMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 17, a", List.of(
            req("celebrant", FieldType.PERSON_REF, "Celebrante")
    )),
    ADM_MNC_TRANSFERENCIA_PAIS(9L, ADMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 17, b", List.of(
            req("originChurch",      FieldType.STRING, "Igreja de origem dos pais"),
            opt("originPresbytery",  FieldType.STRING, "Presbitério de origem")
    )),
    ADM_MNC_JURISDICAO_PAIS(10L, ADMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 17, c", List.of(
            req("originChurch",      FieldType.STRING, "Igreja de origem dos pais"),
            opt("originPresbytery",  FieldType.STRING, "Presbitério de origem")
    )),

    // Demissão de membro comungante (Art. 23)
    DEM_MC_EXCLUSAO_DISCIPLINA(11L, DEMISSAO_MEMBRO_COMUNGANTE, "Art. 23, a", List.of(
            req("reason",                      FieldType.TEXT,   "Motivo / falta disciplinar"),
            opt("disciplinaryProcessNumber",   FieldType.STRING, "Nº do processo disciplinar")
    )),
    DEM_MC_EXCLUSAO_A_PEDIDO(12L, DEMISSAO_MEMBRO_COMUNGANTE, "Art. 23, b", List.of(
            opt("reason", FieldType.TEXT, "Motivo declarado")
    )),
    DEM_MC_EXCLUSAO_AUSENCIA(13L, DEMISSAO_MEMBRO_COMUNGANTE, "Art. 23, c", List.of(
            opt("reason", FieldType.TEXT, "Contexto / tempo de ausência")
    )),
    DEM_MC_CARTA_TRANSFERENCIA(14L, DEMISSAO_MEMBRO_COMUNGANTE, "Art. 23, d", List.of(
            req("destinationChurch",      FieldType.STRING, "Igreja destino"),
            opt("destinationPresbytery",  FieldType.STRING, "Presbitério destino"),
            opt("letterNumber",           FieldType.STRING, "Referência da carta")
    )),
    DEM_MC_JURISDICAO_OUTRA_IGREJA(15L, DEMISSAO_MEMBRO_COMUNGANTE, "Art. 23, e", List.of(
            req("destinationChurch",      FieldType.STRING, "Igreja que assumiu jurisdição"),
            opt("destinationPresbytery",  FieldType.STRING, "Presbitério destino")
    )),
    DEM_MC_FALECIMENTO(16L, DEMISSAO_MEMBRO_COMUNGANTE, "Art. 23, f", List.of(
            opt("funeralCeremonyDate",      FieldType.DATE,   "Data da cerimônia fúnebre"),
            opt("funeralCeremonyLocation",  FieldType.STRING, "Local da cerimônia"),
            opt("funeralCelebrantName",     FieldType.STRING, "Celebrante"),
            opt("causeOfDeath",             FieldType.STRING, "Causa mortis")
    )),
    DEM_MC_ORDENACAO_MINISTRO(17L, DEMISSAO_MEMBRO_COMUNGANTE, "Art. 23, §3", List.of(
            req("originPresbytery",  FieldType.STRING, "Presbitério ao qual o nome foi transferido"),
            opt("resolutionNumber",  FieldType.STRING, "Número da resolução")
    )),

    // Demissão de membro não comungante (Art. 24)
    DEM_MNC_TRANSF_PAIS(18L, DEMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 24, a", List.of(
            req("destinationChurch",      FieldType.STRING, "Igreja destino"),
            opt("destinationPresbytery",  FieldType.STRING, "Presbitério destino")
    )),
    DEM_MNC_TRANSF_PROPRIA(19L, DEMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 24, b", List.of(
            req("destinationChurch",      FieldType.STRING, "Igreja destino"),
            opt("destinationPresbytery",  FieldType.STRING, "Presbitério destino")
    )),
    DEM_MNC_MAIORIDADE(20L, DEMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 24, c", List.of(
            opt("reason", FieldType.TEXT, "Observações")
    )),
    // Caso especial: promove para Membro Comungante (não vai para Ex-membro)
    DEM_MNC_PROFISSAO_FE(21L, DEMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 24, d", List.of(
            req("celebrant", FieldType.PERSON_REF, "Celebrante")
    )),
    DEM_MNC_SOLIC_PAIS_OUTRA(22L, DEMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 24, e", List.of(
            req("destinationChurch",      FieldType.STRING, "Comunidade destino"),
            opt("destinationPresbytery",  FieldType.STRING, "Presbitério destino"),
            opt("reason",                 FieldType.TEXT,   "Motivo")
    )),
    DEM_MNC_FALECIMENTO(23L, DEMISSAO_MEMBRO_NAO_COMUNGANTE, "Art. 24, f", List.of(
            opt("funeralCeremonyDate",      FieldType.DATE,   "Data da cerimônia fúnebre"),
            opt("funeralCeremonyLocation",  FieldType.STRING, "Local"),
            opt("funeralCelebrantName",     FieldType.STRING, "Celebrante"),
            opt("causeOfDeath",             FieldType.STRING, "Causa mortis")
    ));

    private final Long id;
    private final OfficialActTypeEnum type;
    private final String articleClause;
    private final List<MetadataFieldSpec> metadataFields;

    OfficialActFormEnum(Long id, OfficialActTypeEnum type, String articleClause, List<MetadataFieldSpec> metadataFields) {
        this.id = id;
        this.type = type;
        this.articleClause = articleClause;
        this.metadataFields = metadataFields;
    }

    public Long getId() {
        return id;
    }

    public OfficialActTypeEnum getType() {
        return type;
    }

    public String getArticleClause() {
        return articleClause;
    }

    public List<MetadataFieldSpec> getMetadataFields() {
        return metadataFields;
    }

    public boolean isAdmission() {
        return "ADMISSAO".equals(type.getCategory());
    }

    public boolean isPromotionFromMnc() {
        return this == DEM_MNC_PROFISSAO_FE;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static OfficialActFormEnum fromId(Long id) {
        for (OfficialActFormEnum value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Forma de ato oficial não encontrada para o ID: " + id);
    }
}
