package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficialActCreateForm {
    private Long officialActFormId;
    private DateTime actDate;
    private String minuteNumber;
    private DateTime minuteDate;
    private List<Long> personIds;
    private Map<String, Object> metadata;
    private String notes;

    @JsonIgnore
    private boolean skipEffects = false;
    @JsonIgnore
    private boolean skipAdmissionOrderNumber = false;

    public OfficialActCreateForm() {}

    public Long getOfficialActFormId() { return officialActFormId; }
    public void setOfficialActFormId(Long officialActFormId) { this.officialActFormId = officialActFormId; }
    public DateTime getActDate() { return actDate; }
    public void setActDate(DateTime actDate) { this.actDate = actDate; }
    public String getMinuteNumber() { return minuteNumber; }
    public void setMinuteNumber(String minuteNumber) { this.minuteNumber = minuteNumber; }
    public DateTime getMinuteDate() { return minuteDate; }
    public void setMinuteDate(DateTime minuteDate) { this.minuteDate = minuteDate; }
    public List<Long> getPersonIds() { return personIds; }
    public void setPersonIds(List<Long> personIds) { this.personIds = personIds; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isSkipEffects() { return skipEffects; }
    public void setSkipEffects(boolean skipEffects) { this.skipEffects = skipEffects; }
    public boolean isSkipAdmissionOrderNumber() { return skipAdmissionOrderNumber; }
    public void setSkipAdmissionOrderNumber(boolean skipAdmissionOrderNumber) { this.skipAdmissionOrderNumber = skipAdmissionOrderNumber; }
}
