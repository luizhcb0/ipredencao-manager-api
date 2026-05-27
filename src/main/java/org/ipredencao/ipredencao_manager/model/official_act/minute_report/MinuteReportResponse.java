package org.ipredencao.ipredencao_manager.model.official_act.minute_report;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MinuteReportResponse {
    private String minuteNumber;
    private LocalDate minuteDate;
    private List<CategorySection> sections;

    public MinuteReportResponse() {}

    public MinuteReportResponse(String minuteNumber, LocalDate minuteDate, List<CategorySection> sections) {
        this.minuteNumber = minuteNumber;
        this.minuteDate = minuteDate;
        this.sections = sections;
    }

    public String getMinuteNumber() { return minuteNumber; }
    public void setMinuteNumber(String minuteNumber) { this.minuteNumber = minuteNumber; }
    public LocalDate getMinuteDate() { return minuteDate; }
    public void setMinuteDate(LocalDate minuteDate) { this.minuteDate = minuteDate; }
    public List<CategorySection> getSections() { return sections; }
    public void setSections(List<CategorySection> sections) { this.sections = sections; }
}
