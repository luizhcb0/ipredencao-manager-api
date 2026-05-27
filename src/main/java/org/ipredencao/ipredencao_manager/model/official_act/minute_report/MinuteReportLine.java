package org.ipredencao.ipredencao_manager.model.official_act.minute_report;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MinuteReportLine {
    private Long actId;
    private Long personId;
    private Long admissionOrderNumber;
    private String text;

    public MinuteReportLine() {}

    public MinuteReportLine(Long actId, Long personId, Long admissionOrderNumber, String text) {
        this.actId = actId;
        this.personId = personId;
        this.admissionOrderNumber = admissionOrderNumber;
        this.text = text;
    }

    public Long getActId() { return actId; }
    public void setActId(Long actId) { this.actId = actId; }
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public Long getAdmissionOrderNumber() { return admissionOrderNumber; }
    public void setAdmissionOrderNumber(Long admissionOrderNumber) { this.admissionOrderNumber = admissionOrderNumber; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
