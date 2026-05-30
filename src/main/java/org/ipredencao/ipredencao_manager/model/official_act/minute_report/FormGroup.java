package org.ipredencao.ipredencao_manager.model.official_act.minute_report;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormGroup {
    private Long formId;
    private String formName;
    private String articleClause;
    private List<MinuteReportLine> lines;

    public FormGroup() {}

    public FormGroup(Long formId, String formName, String articleClause, List<MinuteReportLine> lines) {
        this.formId = formId;
        this.formName = formName;
        this.articleClause = articleClause;
        this.lines = lines;
    }

    public Long getFormId() { return formId; }
    public void setFormId(Long formId) { this.formId = formId; }
    public String getFormName() { return formName; }
    public void setFormName(String formName) { this.formName = formName; }
    public String getArticleClause() { return articleClause; }
    public void setArticleClause(String articleClause) { this.articleClause = articleClause; }
    public List<MinuteReportLine> getLines() { return lines; }
    public void setLines(List<MinuteReportLine> lines) { this.lines = lines; }
}
