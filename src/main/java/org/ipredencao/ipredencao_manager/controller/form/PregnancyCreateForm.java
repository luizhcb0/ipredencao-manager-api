package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PregnancyCreateForm {

    private Long motherId;
    private Long fatherId;
    private DateTime expectedDueDate;
    private String name;
    private Sexo gender;
    private boolean confidential;

    public Long getMotherId() { return motherId; }
    public void setMotherId(Long motherId) { this.motherId = motherId; }

    public Long getFatherId() { return fatherId; }
    public void setFatherId(Long fatherId) { this.fatherId = fatherId; }

    public DateTime getExpectedDueDate() { return expectedDueDate; }
    public void setExpectedDueDate(DateTime expectedDueDate) { this.expectedDueDate = expectedDueDate; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Sexo getGender() { return gender; }
    public void setGender(Sexo gender) { this.gender = gender; }

    public boolean isConfidential() { return confidential; }
    public void setConfidential(boolean confidential) { this.confidential = confidential; }
}
