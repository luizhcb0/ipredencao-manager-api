package org.ipredencao.ipredencao_manager.model.official_act;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MinuteResponse(String minuteNumber, LocalDate minuteDate) {}
