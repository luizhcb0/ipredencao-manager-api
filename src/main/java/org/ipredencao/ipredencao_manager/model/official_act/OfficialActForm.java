package org.ipredencao.ipredencao_manager.model.official_act;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfficialActForm {
    private Long id;
    private Long officialActTypeId;
    private String name;
    private String articleClause;
    /** Schema hidratado pelo catálogo a partir do {@link OfficialActFormEnum}. */
    private List<MetadataFieldSpec> metadataSchema;

    public OfficialActForm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOfficialActTypeId() { return officialActTypeId; }
    public void setOfficialActTypeId(Long officialActTypeId) { this.officialActTypeId = officialActTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArticleClause() { return articleClause; }
    public void setArticleClause(String articleClause) { this.articleClause = articleClause; }
    public List<MetadataFieldSpec> getMetadataSchema() { return metadataSchema; }
    public void setMetadataSchema(List<MetadataFieldSpec> metadataSchema) { this.metadataSchema = metadataSchema; }
}
