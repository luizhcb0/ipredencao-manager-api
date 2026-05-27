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
    private Long admissionOrderNumber;
    private Map<String, Object> metadata;
    private String notes;
    private DateTime addedAt;
    private DateTime updatedAt;
    private Long updatedBy;

    // Derivados via JOIN
    private Long officialActTypeId;
    private String typeName;
    private String formName;
    private String category;
    private String referenceArticle;
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
    public Long getAdmissionOrderNumber() { return admissionOrderNumber; }
    public void setAdmissionOrderNumber(Long admissionOrderNumber) { this.admissionOrderNumber = admissionOrderNumber; }
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
