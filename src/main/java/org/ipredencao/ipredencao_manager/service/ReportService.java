package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.controller.views.BirthdayEntryView;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.model.pessoa.AgregadorCategoriaEnum;
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

    private static final Set<AgregadorCategoriaEnum> BIRTHDAY_EXCLUDED_AGREGADORES = Set.of(
        AgregadorCategoriaEnum.ROL_A_PARTE,
        AgregadorCategoriaEnum.MISSIONARIO,
        AgregadorCategoriaEnum.POSSIVEL_ADMISSAO_GESTACAO,
        AgregadorCategoriaEnum.PESSOA_REFERENCIADA,
        AgregadorCategoriaEnum.EX_MEMBRO_DA_IGREJA
    );

    @Autowired
    private PessoaRepository pessoaRepository;
    
    @Autowired
    private FormularioPessoaRepository formularioRepository;
    
    public SummaryResponse generateSummary() {
        log.info("Gerando resumo de dados...");
        
        // Buscar todas as pessoas
        List<Pessoa> people = pessoaRepository.find(PessoaQuery.builder().build());
        
        // Buscar todos os formulários
        List<FormularioPessoa> forms = formularioRepository.find(FormularioPessoaQuery.builder().build());
        
        // Contar chefes de família únicos removendo agregadores de categoria específicos
        Long families = people.stream()
            .filter(p -> {
                Long agregadorId = p.getCategoria().getAgregadorCategoriaId();
                return !agregadorId.equals(5L) && !agregadorId.equals(6L) && !agregadorId.equals(7L) && !agregadorId.equals(8L) && !agregadorId.equals(9L) && !agregadorId.equals(10L);
            })
            .map(Pessoa::getChefeDeFamiliaId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        
        // Contar por subcategoria (categoria)
        Map<String, Long> pessoasPorCategoria = people.stream()
            .filter(p -> p.getCategoria() != null)
            .collect(Collectors.groupingBy(
                p -> p.getCategoria().name(), 
                Collectors.counting()
            ));
        
        // Contar formulários por status
        Map<String, Long> formsByStatus = new HashMap<>();
        
        // Inicializar todos os status com 0
        for (FormPessoaStatus status : FormPessoaStatus.values()) {
            formsByStatus.put(status.name(), 0L);
        }
        
        // Contar formulários existentes
        Map<String, Long> formsByStatusCount = forms.stream()
            .filter(f -> f.getStatus() != null)
            .collect(Collectors.groupingBy(
                f -> f.getStatus().name(), 
                Collectors.counting()
            ));
        
        // Atualizar com as contagens
        formsByStatus.putAll(formsByStatusCount);
        
        // Contar pessoas por campus
        Map<String, Long> pessoasPorCampus = people.stream()
            .filter(p -> p.getCampus() != null && !p.getCampus().trim().isEmpty())
            .collect(Collectors.groupingBy(
                Pessoa::getCampus, 
                Collectors.counting()
            ));
        
        // Contar pessoas por sexo
        Map<String, Long> pessoasPorSexo = people.stream()
            .filter(p -> p.getSexo() != null)
            .collect(Collectors.groupingBy(
                p -> p.getSexo().name(), 
                Collectors.counting()
            ));

        SummaryResponse response = new SummaryResponse(
            (long) people.size(),
            (long) forms.size(),
            families,
            pessoasPorCategoria,
            formsByStatus,
            pessoasPorCampus,
            pessoasPorSexo
        );

        response.setBirthdays(filterBirthdays(people, LocalDate.now()));

        return response;
    }

    private boolean isEligibleForBirthday(Pessoa p) {
        if (p.getDataNascimento() == null || p.getDataFalecimento() != null) return false;
        return !BIRTHDAY_EXCLUDED_AGREGADORES.contains(p.getCategoria().getAgregadorCategoria());
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
