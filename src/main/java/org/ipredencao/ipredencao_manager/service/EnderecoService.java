package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.endereco.EnderecoQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pessoa.ConfidentialAccess;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.repository.EnderecoRepository;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EnderecoService {
    
    @Autowired
    private EnderecoRepository enderecoRepository;
    
    @Autowired
    private PessoaRepository pessoaRepository;
    
    @Autowired
    private SecurityUtils securityUtils;
    
    /**
     * Cria um novo endereço e opcionalmente vincula pessoas
     */
    @Transactional
    public Endereco create(Endereco endereco) {
        // Validações
        validateEndereco(endereco);
        
        // Definir quem criou
        Long currentUserId = securityUtils.getCurrentUserId();
        endereco.setUpdatedByUserId(currentUserId);
        
        // Criar endereço
        Endereco created = enderecoRepository.insert(endereco);
        
        // Vincular pessoas, se fornecidas
        if (endereco.getPessoaIds() != null && !endereco.getPessoaIds().isEmpty()) {
            vincularPessoas(created, endereco.getPessoaIds(), currentUserId);
            created.setPessoaIds(endereco.getPessoaIds());
        }
        
        return created;
    }
    
    /**
     * Atualiza um endereço e sincroniza pessoas vinculadas
     */
    @Transactional
    public Endereco update(Endereco endereco) {
        // Validações
        if (endereco.getId() == null) {
            throw new IllegalArgumentException("ID do endereço é obrigatório para atualização");
        }
        
        Endereco existing = findById(endereco.getId());
        if (existing == null) {
            throw new NoSuchElementException("Endereço com ID " + endereco.getId() + " não encontrado");
        }
        
        validateEndereco(endereco);
        
        // Definir quem atualizou
        Long currentUserId = securityUtils.getCurrentUserId();
        endereco.setUpdatedByUserId(currentUserId);
        
        // Obter IDs atuais do endereço existente
        List<Long> currentPersonIds = existing.getPessoaIds() != null ? existing.getPessoaIds() : List.of();
        
        // Se payload tem pessoaIds, validar e sincronizar
        if (endereco.getPessoaIds() != null) {
            // Encontrar pessoas removidas
            List<Long> toRomeve = currentPersonIds.stream()
                .filter(id -> !endereco.getPessoaIds().contains(id))
                .toList();
            
            // Lançar exceção se houver remoções
            if (!toRomeve.isEmpty()) {
                throw new IllegalArgumentException(
                    "Não é possível remover pessoas do endereço. As seguintes pessoas ficariam sem endereço: " 
                    + toRomeve + ". Atribua um novo endereço a elas antes de removê-las deste."
                );
            }
            
            // Encontrar pessoas novas para vincular
            List<Long> toAdd = endereco.getPessoaIds().stream()
                .filter(id -> !currentPersonIds.contains(id))
                .toList();
            
            // Vincular novas pessoas
            if (!toAdd.isEmpty()) {
                vincularPessoas(endereco, toAdd, currentUserId);
            }
        }
        
        // Atualizar dados do endereço
        Endereco updated = enderecoRepository.update(endereco);
        
        // Definir pessoaIds no retorno
        if (endereco.getPessoaIds() != null) {
            updated.setPessoaIds(endereco.getPessoaIds());
        } else {
            // Se não forneceu pessoaIds, manter os atuais
            updated.setPessoaIds(currentPersonIds);
        }
        
        return updated;
    }
    
    /**
     * Deleta um endereço (apenas se não houver pessoas vinculadas)
     */
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID do endereço é obrigatório");
        }
        
        // Verificar se existe
        Endereco existing = findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Endereço com ID " + id + " não encontrado");
        }
        
        // Guard de integridade não depende do perfil: filtrada, a contagem mentiria.
        long count = pessoaRepository.count(
                PessoaQuery.builder().enderecoId(id).includes().build(),
                ConfidentialAccess.INTERNAL);
        if (count > 0) {
            throw new IllegalStateException(
                "Não é possível deletar o endereço. Existem " + count + 
                " pessoa(s) vinculada(s). Desvincule todas as pessoas antes de deletar."
            );
        }
        
        enderecoRepository.delete(id);
    }
    
    /**
     * Busca endereço por ID (incluindo pessoaIds, populado pelo repository)
     */
    public Endereco findById(Long id) {
        if (id == null) {
            return null;
        }
        
        return enderecoRepository.findById(id);
    }
    
    /**
     * Busca endereços com filtros
     */
    public List<Endereco> find(EnderecoQuery query) {
        return enderecoRepository.find(query);
    }
    
    /**
     * Busca endereços com paginação
     */
    public PagedResponse<Endereco> findPaginated(EnderecoQuery query) {
        if (query.getPagination() == null) query.setPagination(new PaginationParameters());
        query.getPagination().applyDefaults();
        
        List<Endereco> enderecos = enderecoRepository.find(query);
        
        long total = enderecoRepository.count(query);
        
        PageInfo pageInfo = new PageInfo(
            query.getPagination().getLimit(),
            query.getPagination().getOffset(),
            total
        );
        
        return new PagedResponse<>(enderecos, pageInfo);
    }
    
    // Métodos privados auxiliares
    
    private void validateEndereco(Endereco endereco) {
        if (endereco.getCep() == null || endereco.getCep().trim().isEmpty()) {
            throw new IllegalArgumentException("CEP é obrigatório");
        }
        
        if (endereco.getLogradouro() == null || endereco.getLogradouro().trim().isEmpty()) {
            throw new IllegalArgumentException("Logradouro é obrigatório");
        }
    }
    
    /**
     * Vincula pessoas a um endereço (usado no create)
     */
    private void vincularPessoas(Endereco endereco, List<Long> pessoaIds, Long currentUserId) {
        for (Long pessoaId : pessoaIds) {
            // Validar que pessoa existe
            try {
                Pessoa pessoa = pessoaRepository.find(
                    PessoaQuery.builder()
                        .id(pessoaId)
                        .includes()
                        .build()
                ).getFirst();

                pessoa.setEndereco(endereco);
                pessoa.setUpdatedByUserId(currentUserId);
                // Atualizar endereco_id da pessoa
                pessoaRepository.update(pessoa);
            } catch (NoSuchElementException e) {
                throw new IllegalArgumentException("Pessoa com ID " + pessoaId + " não encontrada");
            }
        }
    }
    
}

