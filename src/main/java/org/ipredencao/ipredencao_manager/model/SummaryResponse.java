package org.ipredencao.ipredencao_manager.model;

import org.ipredencao.ipredencao_manager.controller.views.BirthdayEntryView;

import java.util.List;
import java.util.Map;

public class SummaryResponse {
    
    private Long totalMembers;
    private Long totalForms;
    private Long families;

    private Map<String, Long> personsByCategory;
    private Map<String, Long> formsByStatus;
    private Map<String, Long> membersByCampus;
    private Map<String, Long> membersBySex;
    private List<BirthdayEntryView> birthdays;
    
    public SummaryResponse() {}
    
    public SummaryResponse(
        Long totalMembers,
        Long totalForms,
        Long families,
        Map<String, Long> personsByCategory,
        Map<String, Long> formsByStatus,
        Map<String, Long> membersByCampus,
        Map<String, Long> membersBySex
    ) {
        this.totalMembers = totalMembers;
        this.totalForms = totalForms;
        this.families = families;
        this.personsByCategory = personsByCategory;
        this.formsByStatus = formsByStatus;
        this.membersByCampus = membersByCampus;
        this.membersBySex = membersBySex;
    }

    public Long getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(Long totalMembers) {
        this.totalMembers = totalMembers;
    }

    public Long getTotalForms() {
        return totalForms;
    }

    public void setTotalForms(Long totalForms) {
        this.totalForms = totalForms;
    }

    public Long getFamilies() {
        return families;
    }

    public void setFamilies(Long families) {
        this.families = families;
    }

    public Map<String, Long> getPersonsByCategory() {
        return personsByCategory;
    }

    public void setPersonsByCategory(Map<String, Long> personsByCategory) {
        this.personsByCategory = personsByCategory;
    }

    public Map<String, Long> getFormsByStatus() {
        return formsByStatus;
    }

    public void setFormsByStatus(Map<String, Long> formsByStatus) {
        this.formsByStatus = formsByStatus;
    }

    public Map<String, Long> getMembersByCampus() {
        return membersByCampus;
    }

    public void setMembersByCampus(Map<String, Long> membersByCampus) {
        this.membersByCampus = membersByCampus;
    }

    public Map<String, Long> getMembersBySex() {
        return membersBySex;
    }

    public void setMembersBySex(Map<String, Long> membersBySex) {
        this.membersBySex = membersBySex;
    }

    public List<BirthdayEntryView> getBirthdays() {
        return birthdays;
    }

    public void setBirthdays(List<BirthdayEntryView> birthdays) {
        this.birthdays = birthdays;
    }
}
