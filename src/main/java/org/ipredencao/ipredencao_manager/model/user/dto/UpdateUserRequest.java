package org.ipredencao.ipredencao_manager.model.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;

import java.io.IOException;

@JsonSerialize(using = UpdateUserRequest.Serializer.class)
public class UpdateUserRequest {

    private String name;
    private PerfilAcesso accessProfile;
    private Boolean active;
    private Long personId;
    private boolean personIdPresent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PerfilAcesso getAccessProfile() {
        return accessProfile;
    }

    public void setAccessProfile(PerfilAcesso accessProfile) {
        this.accessProfile = accessProfile;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getPersonId() {
        return personId;
    }

    @JsonSetter("personId")
    public void setPersonId(Long personId) {
        this.personId = personId;
        this.personIdPresent = true;
    }

    @JsonIgnore
    public boolean isPersonIdPresent() {
        return personIdPresent;
    }

    // omitir personId ≠ enviar null
    static final class Serializer extends JsonSerializer<UpdateUserRequest> {
        @Override
        public void serialize(UpdateUserRequest value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            if (value.getName() != null) {
                gen.writeStringField("name", value.getName());
            }
            if (value.getAccessProfile() != null) {
                gen.writeStringField("accessProfile", value.getAccessProfile().name());
            }
            if (value.getActive() != null) {
                gen.writeBooleanField("active", value.getActive());
            }
            if (value.isPersonIdPresent()) {
                gen.writeObjectField("personId", value.getPersonId());
            }
            gen.writeEndObject();
        }
    }
}
