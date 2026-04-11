package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.controller.views.BirthdayEntryView;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.model.pessoa.AgregadorCategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.IgrejaCampus;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.FormPessoaStatus;
import org.ipredencao.ipredencao_manager.repository.FormularioPessoaRepository;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final Set<AgregadorCategoriaEnum> MEMBER_AGGREGGATORS = Set.of(
        AgregadorCategoriaEnum.PASTOR,
        AgregadorCategoriaEnum.MEMBRO_COMUNGANTE,
        AgregadorCategoriaEnum.MEMBRO_NAO_COMUNGANTE,
        AgregadorCategoriaEnum.ROL_A_PARTE
    );

    private static final Set<AgregadorCategoriaEnum> BIRTHDAY_AGGREGGATORS = EnumSet.of(
        AgregadorCategoriaEnum.PASTOR,
        AgregadorCategoriaEnum.MEMBRO_COMUNGANTE,
        AgregadorCategoriaEnum.MEMBRO_NAO_COMUNGANTE,
        AgregadorCategoriaEnum.ROL_A_PARTE,
        AgregadorCategoriaEnum.AGREGADO_NAO_MEMBRO
    );

    @Autowired
    private PessoaRepository pessoaRepository;
    
    @Autowired
    private FormularioPessoaRepository formularioRepository;
    
    public SummaryResponse generateSummary() {
        log.info("Gerando resumo de dados...");

        List<Pessoa> people = pessoaRepository.find(PessoaQuery.builder().includes().build());
        List<FormularioPessoa> forms = formularioRepository.find(FormularioPessoaQuery.builder().build());
        
        List<Pessoa> members = people.stream()
            .filter(p -> MEMBER_AGGREGGATORS.contains(p.getCategoria().getAgregadorCategoria()))
            .toList();

        Long families = members.stream()
            .map(Pessoa::getChefeDeFamiliaId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        
        Map<String, Long> peopleByCategory = people.stream()
            .filter(p -> p.getCategoria() != null)
            .collect(Collectors.groupingBy(
                p -> p.getCategoria().name(), 
                Collectors.counting()
            ));
        
        Map<String, Long> formsByStatus = new HashMap<>();
        for (FormPessoaStatus status : FormPessoaStatus.values()) {
            formsByStatus.put(status.name(), 0L);
        }
        formsByStatus.putAll(
            forms.stream().filter(f -> f.getStatus() != null)
                .collect(Collectors.groupingBy(
                    f -> f.getStatus().name(), Collectors.counting()
                ))
        );
        
        Map<String, Long> byCampus = members.stream()
            .filter(p -> p.getCampus() != null && !p.getCampus().trim().isEmpty())
            .collect(Collectors.groupingBy(
                Pessoa::getCampus, 
                Collectors.counting()
            ));
        
        Map<String, Long> bySex = members.stream()
            .filter(p -> p.getSexo() != null)
            .collect(Collectors.groupingBy(
                p -> p.getSexo().name(), 
                Collectors.counting()
            ));

        SummaryResponse response = new SummaryResponse(
            (long) members.size(),
            (long) forms.size(),
            families,
            peopleByCategory,
            formsByStatus,
            byCampus,
            bySex
        );

        response.setBirthdays(filterBirthdays(people, LocalDate.now()));

        return response;
    }

    private boolean isEligibleForBirthday(Pessoa p) {
        if (p.getDataNascimento() == null || p.getDataFalecimento() != null) return false;
        if (!IgrejaCampus.SEDE.name().equals(p.getCampus())) return false;
        return BIRTHDAY_AGGREGGATORS.contains(p.getCategoria().getAgregadorCategoria());
    }

    private List<BirthdayEntryView> filterBirthdays(List<Pessoa> people, LocalDate today) {
        LocalDate from = today.minusDays(2);
        LocalDate to = today.plusDays(7);

        Map<MonthDay, LocalDate> targetDays = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            targetDays.put(MonthDay.from(d), d);
        }

        return people.stream()
            .filter(this::isEligibleForBirthday)
            .map(p -> {
                MonthDay md = MonthDay.of(
                    p.getDataNascimento().getMonthOfYear(),
                    p.getDataNascimento().getDayOfMonth()
                );
                LocalDate birthdayDate = targetDays.get(md);
                if (birthdayDate == null) return null;
                int age = birthdayDate.getYear() - p.getDataNascimento().getYear();
                return new BirthdayEntryView(
                    p.getId(), p.getNome(), p.getFotoUrl(),
                    p.getDataNascimento(), birthdayDate.toString(), age
                );
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(BirthdayEntryView::getBirthdayDate))
            .collect(Collectors.toList());
    }
}
