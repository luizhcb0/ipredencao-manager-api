package org.ipredencao.ipredencao_manager.model;

import org.ipredencao.ipredencao_manager.controller.views.BirthdayEntryView;

import java.util.List;
import java.util.Map;

public class SummaryResponse {
    
    private Long totalPersons;
    private Long totalForms;
    private Long families;

    private Map<String, Long> personsByCategory;
    private Map<String, Long> formsByStatus;
    private Map<String, Long> personsByCampus;
    private Map<String, Long> personsBySex;
    private List<BirthdayEntryView> birthdays;
    
    public SummaryResponse() {}
    
    public SummaryResponse(
        Long totalPersons,
        Long totalForms,
        Long families,
        Map<String, Long> personsByCategory,
        Map<String, Long> formsByStatus,
        Map<String, Long> personsByCampus,
        Map<String, Long> personsBySex
    ) {
        this.totalPersons = totalPersons;
        this.totalForms = totalForms;
        this.families = families;
        this.personsByCategory = personsByCategory;
        this.formsByStatus = formsByStatus;
        this.personsByCampus = personsByCampus;
        this.personsBySex = personsBySex;
    }

    public Long getTotalPersons() {
        return totalPersons;
    }

    public void setTotalPersons(Long totalPersons) {
        this.totalPersons = totalPersons;
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

    public Map<String, Long> getPersonsByCampus() {
        return personsByCampus;
    }

    public void setPersonsByCampus(Map<String, Long> personsByCampus) {
        this.personsByCampus = personsByCampus;
    }

    public Map<String, Long> getPersonsBySex() {
        return personsBySex;
    }

    public void setPersonsBySex(Map<String, Long> personsBySex) {
        this.personsBySex = personsBySex;
    }

    public List<BirthdayEntryView> getBirthdays() {
        return birthdays;
    }

    public void setBirthdays(List<BirthdayEntryView> birthdays) {
        this.birthdays = birthdays;
    }
}
