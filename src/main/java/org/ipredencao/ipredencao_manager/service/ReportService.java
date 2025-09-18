package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.FormPessoaStatus;
import org.ipredencao.ipredencao_manager.repository.FormularioPessoaRepository;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    
    @Autowired
    private PessoaRepository pessoaRepository;
    
    @Autowired
    private FormularioPessoaRepository formularioRepository;
    
    public SummaryResponse generateSummary() {
        log.info("Gerando resumo de dados...");
        
        // Buscar todas as pessoas
        List<Pessoa> pessoas = pessoaRepository.find(PessoaQuery.builder().build());
        
        // Buscar todos os formulários
        List<FormularioPessoa> formularios = formularioRepository.find(FormularioPessoaQuery.builder().build());
        
        // Contar chefes de família (pessoas que têm outras pessoas dependentes delas)
        Set<Long> chefesDeFamiliaIds = pessoas.stream()
            .map(Pessoa::getChefeDeFamiliaId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Long totalFamilias = (long) chefesDeFamiliaIds.size();
        
        // Contar por subcategoria (categoria)
        Map<String, Long> pessoasPorCategoria = pessoas.stream()
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
        Map<String, Long> formsByStatusCount = formularios.stream()
            .filter(f -> f.getStatus() != null)
            .collect(Collectors.groupingBy(
                f -> f.getStatus().name(), 
                Collectors.counting()
            ));
        
        // Atualizar com as contagens
        formsByStatus.putAll(formsByStatusCount);
        
        // Contar pessoas por região
        Map<String, Long> pessoasPorRegiao = pessoas.stream()
            .filter(p -> p.getRegiao() != null)
            .collect(Collectors.groupingBy(
                p -> p.getRegiao().name(), 
                Collectors.counting()
            ));
        
        // Contar pessoas por campus
        Map<String, Long> pessoasPorCampus = pessoas.stream()
            .filter(p -> p.getCampus() != null && !p.getCampus().trim().isEmpty())
            .collect(Collectors.groupingBy(
                Pessoa::getCampus, 
                Collectors.counting()
            ));
        
        // Contar pessoas por sexo
        Map<String, Long> pessoasPorSexo = pessoas.stream()
            .filter(p -> p.getSexo() != null)
            .collect(Collectors.groupingBy(
                p -> p.getSexo().name(), 
                Collectors.counting()
            ));

        SummaryResponse resumo = new SummaryResponse(
            (long) pessoas.size(),
            (long) formularios.size(),
            totalFamilias,
            pessoasPorCategoria,
            formsByStatus,
            pessoasPorRegiao,
            pessoasPorCampus,
            pessoasPorSexo
        );

        return resumo;
    }
}
