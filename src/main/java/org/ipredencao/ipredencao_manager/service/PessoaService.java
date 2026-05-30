package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.endereco.EnderecoQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistory;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaInclude;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
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
    @Autowired
    private EnderecoService enderecoService;

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public Pessoa create(Pessoa pessoa) {
        // Definir quem criou a pessoa
        Long currentUserId = securityUtils.getCurrentUserId();
        pessoa.setUpdatedByUserId(currentUserId);
        
        // Criar ou reutilizar endereço se fornecido e não tem ID
        if (pessoa.getEndereco() != null && pessoa.getEndereco().getId() == null) {
            // Verificar se o endereço tem CEP E logradouro (ambos obrigatórios)
            boolean hasCep = pessoa.getEndereco().getCep() != null && !pessoa.getEndereco().getCep().trim().isEmpty();
            boolean hasLogradouro = pessoa.getEndereco().getLogradouro() != null && !pessoa.getEndereco().getLogradouro().trim().isEmpty();
            
            if (hasCep && hasLogradouro) {
                // Buscar endereço existente pelo logradouro
                Endereco enderecoExistente = buscarEnderecoExistente(pessoa.getEndereco());
                
                if (enderecoExistente != null) {
                    // Reutilizar endereço existente
                    pessoa.setEndereco(enderecoExistente);
                } else {
                    // Criar novo endereço
                    pessoa.getEndereco().setUpdatedByUserId(currentUserId);
                    Endereco enderecoCriado = enderecoService.create(pessoa.getEndereco());
                    pessoa.setEndereco(enderecoCriado);
                }
            } else {
                // Endereço sem dados suficientes (precisa CEP E logradouro), remover
                pessoa.setEndereco(null);
            }
        }
        
        return pessoaRepository.insert(pessoa);
    }
    
    /**
     * Busca um endereço existente pelo logradouro (e CEP se disponível)
     */
    private Endereco buscarEnderecoExistente(Endereco endereco) {
        if (endereco.getLogradouro() == null || endereco.getLogradouro().trim().isEmpty()) {
            return null;
        }
        
        // Buscar por logradouro e CEP (se disponível)
        EnderecoQuery.Builder queryBuilder = EnderecoQuery.builder()
            .logradouro(endereco.getLogradouro().trim());
        
        if (endereco.getCep() != null && !endereco.getCep().trim().isEmpty()) {
            queryBuilder.cep(endereco.getCep().trim());
        }
        
        List<Endereco> enderecos = enderecoService.find(queryBuilder.build());
        
        // Retornar o primeiro encontrado, ou null se nenhum foi encontrado
        return enderecos.isEmpty() ? null : enderecos.get(0);
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

    /**
     * Busca várias pessoas por IDs em uma única query ({@code WHERE pessoa_id IN (?)}).
     * Inclui endereço e relacionamentos — suficiente para o {@code MinuteReportFormatter}.
     */
    public List<Pessoa> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return pessoaRepository.find(
                PessoaQuery.builder()
                        .ids(List.copyOf(ids))
                        .includes(PessoaInclude.ENDERECO, PessoaInclude.RELACIONAMENTOS)
                        .build());
    }

    public List<Pessoa> find(PessoaQuery query) {
        return pessoaRepository.find(query);
    }
    
    /**
     * Busca pessoas com paginação
     */
    public PagedResponse<Pessoa> findPaginated(PessoaQuery query) {
        if (query.getPagination() == null) query.setPagination(new PaginationParameters());
        query.getPagination().applyDefaults();
        
        List<Pessoa> pessoas = pessoaRepository.find(query);
        long total = pessoaRepository.count(query);
        
        PageInfo pageInfo = new PageInfo(
            query.getPagination().getLimit(),
            query.getPagination().getOffset(),
            total
        );
        
        return new PagedResponse<>(pessoas, pageInfo);
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
    /**
     * Cria um relacionamento entre duas pessoas.
     * Se o relacionamento já existir (em qualquer direção), retorna o existente.
     * Comportamento idempotente para evitar duplicações.
     */
    public Relacionamento createRelationship(Long pessoaId, Relacionamento relacionamento) {
        // Verificar se relacionamento já existe (em qualquer direção)
        Relacionamento existingRelationship = pessoaRepository.findExistingRelationship(
            pessoaId, 
            relacionamento.getPessoaRelacionadaId(), 
            relacionamento.getTipoRelacionamento()
        );
        
        // Se já existe, retornar o existente (comportamento idempotente)
        if (existingRelationship != null) {
            return existingRelationship;
        }
        
        // Se não existe, criar novo
        return pessoaRepository.insertRelationship(pessoaId, relacionamento);
    }
    
    /**
     * Busca o histórico de alterações de uma pessoa
     */
    public List<PessoaHistory> findHistoryById(Long id) {
        return pessoaRepository.findHistoryByPersonId(id);
    }

    /** Retorna a categoria_id em vigor para a pessoa imediatamente antes do timestamp informado. */
    public Optional<Long> findCategoriaIdBefore(Long pessoaId, DateTime threshold) {
        return pessoaRepository.findCategoriaIdBefore(pessoaId, threshold);
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
            newPerson.setCategoria(CategoriaEnum.PESSOA_REFERENCIADA);
            Pessoa createdPerson = pessoaRepository.insert(newPerson);
            rel.setPessoaRelacionadaId(createdPerson.getId());
        }
    }
    
}
