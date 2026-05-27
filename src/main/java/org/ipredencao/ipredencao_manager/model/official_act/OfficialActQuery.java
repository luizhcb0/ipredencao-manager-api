package org.ipredencao.ipredencao_manager.model.official_act;

import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.joda.time.DateTime;

import java.util.List;

public class OfficialActQuery {
    private String minuteNumber;
    private DateTime startDate;
    private DateTime endDate;
    private List<Long> typeIds;
    private List<Long> formIds;
    private Long personId;
    private String personName;
    private PaginationParameters pagination;

    public OfficialActQuery() {}

    public String getMinuteNumber() { return minuteNumber; }
    public void setMinuteNumber(String minuteNumber) { this.minuteNumber = minuteNumber; }
    public DateTime getStartDate() { return startDate; }
    public void setStartDate(DateTime startDate) { this.startDate = startDate; }
    public DateTime getEndDate() { return endDate; }
    public void setEndDate(DateTime endDate) { this.endDate = endDate; }
    public List<Long> getTypeIds() { return typeIds; }
    public void setTypeIds(List<Long> typeIds) { this.typeIds = typeIds; }
    public List<Long> getFormIds() { return formIds; }
    public void setFormIds(List<Long> formIds) { this.formIds = formIds; }
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public PaginationParameters getPagination() { return pagination; }
    public void setPagination(PaginationParameters pagination) { this.pagination = pagination; }
}
