package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MinuteUpdateForm(LocalDate minuteDate) {}
