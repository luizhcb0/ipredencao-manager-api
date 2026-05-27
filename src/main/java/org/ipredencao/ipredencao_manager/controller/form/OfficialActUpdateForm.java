package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.LocalDate;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficialActUpdateForm {
    private String minuteNumber;
    private LocalDate minuteDate;
    private String notes;
    private Map<String, Object> metadata;

    public OfficialActUpdateForm() {}

    public String getMinuteNumber() { return minuteNumber; }
    public void setMinuteNumber(String minuteNumber) { this.minuteNumber = minuteNumber; }
    public LocalDate getMinuteDate() { return minuteDate; }
    public void setMinuteDate(LocalDate minuteDate) { this.minuteDate = minuteDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
