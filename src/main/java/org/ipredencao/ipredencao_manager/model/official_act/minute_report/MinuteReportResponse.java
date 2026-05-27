package org.ipredencao.ipredencao_manager.model.official_act.minute_report;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MinuteReportResponse {
    private String minuteNumber;
    private DateTime minuteDate;
    private List<CategorySection> sections;

    public MinuteReportResponse() {}

    public MinuteReportResponse(String minuteNumber, DateTime minuteDate, List<CategorySection> sections) {
        this.minuteNumber = minuteNumber;
        this.minuteDate = minuteDate;
        this.sections = sections;
    }

    public String getMinuteNumber() { return minuteNumber; }
    public void setMinuteNumber(String minuteNumber) { this.minuteNumber = minuteNumber; }
    public DateTime getMinuteDate() { return minuteDate; }
    public void setMinuteDate(DateTime minuteDate) { this.minuteDate = minuteDate; }
    public List<CategorySection> getSections() { return sections; }
    public void setSections(List<CategorySection> sections) { this.sections = sections; }
}
