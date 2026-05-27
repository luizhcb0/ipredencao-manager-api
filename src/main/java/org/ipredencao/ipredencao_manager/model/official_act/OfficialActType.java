package org.ipredencao.ipredencao_manager.model.official_act;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfficialActType {
    private Long id;
    private String name;
    private String category;
    private String referenceArticle;
    private List<OfficialActForm> forms;

    public OfficialActType() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReferenceArticle() { return referenceArticle; }
    public void setReferenceArticle(String referenceArticle) { this.referenceArticle = referenceArticle; }
    public List<OfficialActForm> getForms() { return forms; }
    public void setForms(List<OfficialActForm> forms) { this.forms = forms; }
}
