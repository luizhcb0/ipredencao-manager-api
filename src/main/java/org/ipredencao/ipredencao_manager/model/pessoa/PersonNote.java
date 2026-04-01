package org.ipredencao.ipredencao_manager.model.pessoa;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonNote {
    private Long id;
    private Long pessoaId;
    private String content;
    private DateTime addedAt;
    private DateTime updatedAt;
    private Long updatedBy;

    public PersonNote() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPessoaId() { return pessoaId; }
    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public DateTime getAddedAt() { return addedAt; }
    public void setAddedAt(DateTime addedAt) { this.addedAt = addedAt; }
    public DateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(DateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
