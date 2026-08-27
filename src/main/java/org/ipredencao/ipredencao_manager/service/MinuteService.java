package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.official_act.Minute;
import org.ipredencao.ipredencao_manager.model.official_act.MinuteResponse;
import org.ipredencao.ipredencao_manager.repository.MinuteRepository;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class MinuteService {

    @Autowired
    private MinuteRepository minuteRepository;

    @Transactional
    public MinuteResponse updateDate(String rawNumber, LocalDate date) {
        if (rawNumber == null || rawNumber.isBlank()) {
            throw new IllegalArgumentException("minuteNumber é obrigatório");
        }
        String number = rawNumber.trim();
        Minute existing = minuteRepository.findByNumber(number);
        if (existing == null) {
            throw new NoSuchElementException("Nenhuma ata encontrada com o número " + number);
        }
        Minute updated = minuteRepository.update(new Minute(number, date));
        return new MinuteResponse(updated.number(), updated.date());
    }
}
