package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistory;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;

@Service
public class PessoaService {
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private SecurityUtils securityUtils;

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public Pessoa create(Pessoa pessoa) {
        // Definir quem criou a pessoa
        Long currentUserId = securityUtils.getCurrentUserId();
        pessoa.setUpdatedByUserId(currentUserId);
        
        return pessoaRepository.insert(pessoa);
    }

    @Transactional
    public Pessoa savePhoto(Pessoa pessoa, MultipartFile foto) throws IOException {
        String fotoUrl = s3Service.uploadPhoto(pessoa.getId(), foto);
        pessoa.setFotoUrl(fotoUrl);
        
        // Definir quem atualizou a pessoa
        Long currentUserId = securityUtils.getCurrentUserId();
        pessoa.setUpdatedByUserId(currentUserId);
        
        return pessoaRepository.update(pessoa);
    }

    public Pessoa findById(Long id) {
        try {
            return pessoaRepository.find(PessoaQuery.builder().id(id).build()).getFirst();
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Pessoa com ID " + id + " não encontrada");
        }
    }

    public List<Pessoa> find(PessoaQuery query) {
        return pessoaRepository.find(query);
    }

    @Transactional
    public Pessoa update(Pessoa pessoa) {
        // Definir quem atualizou a pessoa
        Long currentUserId = securityUtils.getCurrentUserId();
        pessoa.setUpdatedByUserId(currentUserId);
        
        // Buscar pessoa existente para comparar relacionamentos
        Pessoa current = findById(pessoa.getId());
        
        // Atualizar pessoa no banco
        Pessoa updated = pessoaRepository.update(pessoa);
        
        // Sincronizar relacionamentos se existirem
        if (pessoa.getRelacionamentos() != null) {
            syncRelationships(pessoa.getId(), current.getRelacionamentos(), pessoa.getRelacionamentos());
        }
        
        return updated;
    }

    // Relacionamento qualificados
    public Relacionamento createRelationship(Long pessoaId, Relacionamento relacionamento) {
        return pessoaRepository.insertRelationship(pessoaId, relacionamento);
    }
    
    /**
     * Busca o histórico de alterações de uma pessoa
     */
    public List<PessoaHistory> findHistoryById(Long id) {
        return pessoaRepository.findHistoryByPersonId(id);
    }
    
    private void syncRelationships(Long pessoaId, List<Relacionamento> existingRelationships, List<Relacionamento> newRelationships) {
        // Criar mapas para facilitar a comparação
        Map<String, Relacionamento> existingMap = existingRelationships.stream()
            .collect(Collectors.toMap(
                rel -> rel.getPessoaRelacionadaId() + "_" + rel.getTipoRelacionamento().name(),
                rel -> rel
            ));
        
        Map<String, Relacionamento> newMap = newRelationships.stream()
            .collect(Collectors.toMap(
                rel -> rel.getPessoaRelacionadaId() + "_" + rel.getTipoRelacionamento().name(),
                rel -> rel
            ));
        
        // Identificar relacionamentos para remover (existem no banco mas não na nova lista)
        List<String> toRemove = existingMap.keySet().stream()
            .filter(key -> !newMap.containsKey(key))
            .collect(Collectors.toList());
        
        // Identificar relacionamentos para adicionar (existem na nova lista mas não no banco)
        List<String> toAdd = newMap.keySet().stream()
            .filter(key -> !existingMap.containsKey(key))
            .collect(Collectors.toList());
        
        // Identificar relacionamentos para atualizar (existem em ambos mas com dados diferentes)
        List<String> toUpdate = newMap.keySet().stream()
            .filter(key -> existingMap.containsKey(key))
            .filter(key -> !areRelationshipsEqual(existingMap.get(key), newMap.get(key)))
            .collect(Collectors.toList());
        
        // Executar as operações
        for (String key : toRemove) {
            Relacionamento rel = existingMap.get(key);
            pessoaRepository.deleteRelationship(pessoaId, rel.getPessoaRelacionadaId(), rel.getTipoRelacionamento());
        }
        
        for (String key : toAdd) {
            Relacionamento rel = newMap.get(key);
            // Processar pessoa relacionada se necessário
            processRelatedPersonInRelationship(rel);
            pessoaRepository.insertRelationship(pessoaId, rel);
        }
        
        for (String key : toUpdate) {
            Relacionamento existingRel = existingMap.get(key);
            Relacionamento newRel = newMap.get(key);
            pessoaRepository.updateRelationship(pessoaId, existingRel, newRel);
        }
    }
    
    private boolean areRelationshipsEqual(Relacionamento rel1, Relacionamento rel2) {
        return Objects.equals(rel1.getPessoaRelacionadaId(), rel2.getPessoaRelacionadaId()) &&
               rel1.getTipoRelacionamento() == rel2.getTipoRelacionamento() &&
               Objects.equals(rel1.getInicioRelacionamento(), rel2.getInicioRelacionamento());
    }
    
    private void processRelatedPersonInRelationship(Relacionamento rel) {
        // Se pessoaRelacionadaId é null mas nomePessoaRelacionada não é, criar nova pessoa
        if (rel.getPessoaRelacionadaId() == null && rel.getNomePessoaRelacionada() != null && !rel.getNomePessoaRelacionada().trim().isEmpty()) {
            Pessoa newPerson = new Pessoa();
            newPerson.setNome(rel.getNomePessoaRelacionada().trim());
            newPerson.setCategoria(CategoriaEnum.AGREGADO_FAMILIAR);
            Pessoa createdPerson = pessoaRepository.insert(newPerson);
            rel.setPessoaRelacionadaId(createdPerson.getId());
        }
    }
}
