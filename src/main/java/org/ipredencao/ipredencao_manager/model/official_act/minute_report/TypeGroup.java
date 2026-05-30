package org.ipredencao.ipredencao_manager.model.official_act.minute_report;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TypeGroup {
    private Long typeId;
    private String typeName;
    private String referenceArticle;
    private List<FormGroup> forms;

    public TypeGroup() {}

    public TypeGroup(Long typeId, String typeName, String referenceArticle, List<FormGroup> forms) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.referenceArticle = referenceArticle;
        this.forms = forms;
    }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getReferenceArticle() { return referenceArticle; }
    public void setReferenceArticle(String referenceArticle) { this.referenceArticle = referenceArticle; }
    public List<FormGroup> getForms() { return forms; }
    public void setForms(List<FormGroup> forms) { this.forms = forms; }
}
