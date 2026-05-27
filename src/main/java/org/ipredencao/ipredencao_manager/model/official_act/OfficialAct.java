package org.ipredencao.ipredencao_manager.model.official_act;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfficialAct {
    private Long id;
    private Long officialActFormId;
    private Long personId;
    private DateTime actDate;
    private String minuteNumber;
    private DateTime minuteDate;
    /** Só preenchido em admissões (Tipo 1/2) e na promoção MNC→MC (Art. 24, d, que herda). */
    private Long numeroOrdemAdmissao;
    private Map<String, Object> metadata;
    private String notes;
    private DateTime addedAt;
    private DateTime updatedAt;
    private Long updatedBy;

    // Derivados via JOIN (não persistidos diretamente em official_act)
    private Long officialActTypeId;
    private String typeName;
    private String formName;
    private String category;
    /** Artigo do TIPO (granularidade ampla) — ex.: "Art. 16". Vem de {@code official_act_type.reference_article}. */
    private String referenceArticle;
    /** Artigo+alínea da FORMA (granularidade fina) — ex.: "Art. 16, b". Vem de {@code official_act_form.article_clause}. */
    private String articleClause;
    private String personName;
    private Long currentCategoryId;
    private String currentCategoryName;

    public OfficialAct() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOfficialActFormId() { return officialActFormId; }
    public void setOfficialActFormId(Long officialActFormId) { this.officialActFormId = officialActFormId; }
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public DateTime getActDate() { return actDate; }
    public void setActDate(DateTime actDate) { this.actDate = actDate; }
    public String getMinuteNumber() { return minuteNumber; }
    public void setMinuteNumber(String minuteNumber) { this.minuteNumber = minuteNumber; }
    public DateTime getMinuteDate() { return minuteDate; }
    public void setMinuteDate(DateTime minuteDate) { this.minuteDate = minuteDate; }
    public Long getNumeroOrdemAdmissao() { return numeroOrdemAdmissao; }
    public void setNumeroOrdemAdmissao(Long numeroOrdemAdmissao) { this.numeroOrdemAdmissao = numeroOrdemAdmissao; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public DateTime getAddedAt() { return addedAt; }
    public void setAddedAt(DateTime addedAt) { this.addedAt = addedAt; }
    public DateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(DateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public Long getOfficialActTypeId() { return officialActTypeId; }
    public void setOfficialActTypeId(Long officialActTypeId) { this.officialActTypeId = officialActTypeId; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getFormName() { return formName; }
    public void setFormName(String formName) { this.formName = formName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReferenceArticle() { return referenceArticle; }
    public void setReferenceArticle(String referenceArticle) { this.referenceArticle = referenceArticle; }
    public String getArticleClause() { return articleClause; }
    public void setArticleClause(String articleClause) { this.articleClause = articleClause; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public Long getCurrentCategoryId() { return currentCategoryId; }
    public void setCurrentCategoryId(Long currentCategoryId) { this.currentCategoryId = currentCategoryId; }
    public String getCurrentCategoryName() { return currentCategoryName; }
    public void setCurrentCategoryName(String currentCategoryName) { this.currentCategoryName = currentCategoryName; }
}
