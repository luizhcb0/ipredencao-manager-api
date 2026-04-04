package org.ipredencao.ipredencao_manager.controller.views;

import org.joda.time.DateTime;

public class BirthdayEntryView {

    private Long personId;
    private String name;
    private String photoUrl;
    private DateTime birthDate;
    private String birthdayDate;
    private int age;

    public BirthdayEntryView(Long personId, String name, String photoUrl,
                             DateTime birthDate, String birthdayDate, int age) {
        this.personId = personId;
        this.name = name;
        this.photoUrl = photoUrl;
        this.birthDate = birthDate;
        this.birthdayDate = birthdayDate;
        this.age = age;
    }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public DateTime getBirthDate() { return birthDate; }
    public void setBirthDate(DateTime birthDate) { this.birthDate = birthDate; }

    public String getBirthdayDate() { return birthdayDate; }
    public void setBirthdayDate(String birthdayDate) { this.birthdayDate = birthdayDate; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
