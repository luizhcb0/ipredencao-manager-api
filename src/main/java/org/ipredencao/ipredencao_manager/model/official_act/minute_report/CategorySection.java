package org.ipredencao.ipredencao_manager.model.official_act.minute_report;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategorySection {
    private String category;
    private String title;
    private List<TypeGroup> types;

    public CategorySection() {}

    public CategorySection(String category, String title, List<TypeGroup> types) {
        this.category = category;
        this.title = title;
        this.types = types;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<TypeGroup> getTypes() { return types; }
    public void setTypes(List<TypeGroup> types) { this.types = types; }
}
