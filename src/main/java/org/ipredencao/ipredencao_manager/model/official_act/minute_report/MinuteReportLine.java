package org.ipredencao.ipredencao_manager.model.official_act.minute_report;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MinuteReportLine {
    private Long actId;
    private Long personId;
    private Long numeroOrdemAdmissao;
    private String text;

    public MinuteReportLine() {}

    public MinuteReportLine(Long actId, Long personId, Long numeroOrdemAdmissao, String text) {
        this.actId = actId;
        this.personId = personId;
        this.numeroOrdemAdmissao = numeroOrdemAdmissao;
        this.text = text;
    }

    public Long getActId() { return actId; }
    public void setActId(Long actId) { this.actId = actId; }
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public Long getNumeroOrdemAdmissao() { return numeroOrdemAdmissao; }
    public void setNumeroOrdemAdmissao(Long numeroOrdemAdmissao) { this.numeroOrdemAdmissao = numeroOrdemAdmissao; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
