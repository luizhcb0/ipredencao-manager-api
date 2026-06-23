package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PregnancyUpdateForm {

    private Long motherId;
    private Long fatherId;
    /** Opcional para registros legados sem DPP. Ignorado quando {@code birthDate} está preenchida. */
    private DateTime expectedDueDate;
    /** Nome opcional do bebê na gestação; nome completo obrigatório ao registrar nascimento. */
    private String name;
    private Sexo gender;
    /** Ignorado quando {@code birthDate} está preenchida. */
    private Boolean confidential;
    /** Quando preenchida, registra nascimento e altera categoria (default 16). */
    private DateTime birthDate;
    /** Categoria após nascimento. Default: 16 (Aguardando batismo infantil). */
    private Long targetCategoryId;

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

    public Boolean getConfidential() { return confidential; }
    public void setConfidential(Boolean confidential) { this.confidential = confidential; }

    public DateTime getBirthDate() { return birthDate; }
    public void setBirthDate(DateTime birthDate) { this.birthDate = birthDate; }

    public Long getTargetCategoryId() { return targetCategoryId; }
    public void setTargetCategoryId(Long targetCategoryId) { this.targetCategoryId = targetCategoryId; }
}
