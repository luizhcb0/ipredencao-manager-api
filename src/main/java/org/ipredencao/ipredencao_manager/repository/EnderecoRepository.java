package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import static org.ipredencao.ipredencao_manager.jooq.tables.Endereco.ENDERECO;
import static org.ipredencao.ipredencao_manager.jooq.tables.Pessoa.PESSOA;
import org.ipredencao.ipredencao_manager.jooq.tables.records.EnderecoRecord;
import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.pessoa.ConfidentialAccess;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.ipredencao.ipredencao_manager.model.endereco.EnderecoQuery;

@Repository
public class EnderecoRepository {
    @Autowired
    private DSLContext dsl;

    public Endereco insert(Endereco endereco) {
        EnderecoRecord record = toRepository(endereco);
        
        EnderecoRecord saved = dsl.insertInto(ENDERECO)
                .set(record)
                .returning()
                .fetchOne();
        
        Endereco created = fromRepository(saved);
        if (created != null) getPersonIdsFromAddresses(List.of(created));
        return created;
    }

    public Endereco update(Endereco endereco) {
        EnderecoRecord record = toRepository(endereco);
        
        EnderecoRecord updated = dsl.update(ENDERECO)
                .set(record)
                .where(ENDERECO.ID.eq(endereco.getId()))
                .returning()
                .fetchOne();
        
        Endereco result = fromRepository(updated);
        if (result != null) getPersonIdsFromAddresses(List.of(result));
        return result;
    }

    public Endereco findById(Long id) {
        if (id == null) return null;
        
        EnderecoRecord record = dsl.selectFrom(ENDERECO)
                .where(ENDERECO.ID.eq(id))
                .fetchOne();
        
        Endereco endereco = fromRepository(record);
        if (endereco != null) getPersonIdsFromAddresses(List.of(endereco));
        return endereco;
    }

    public List<Endereco> find(EnderecoQuery query) {
        List<Condition> conditions = buildConditions(query);
        
        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);

        // Montar query com ou sem paginação
        List<Endereco> enderecos;
        if (query.getPagination() != null) {
            int limit = query.getPagination().getLimit() != null ? query.getPagination().getLimit() : Integer.MAX_VALUE;
            int offset = query.getPagination().getOffset() != null ? query.getPagination().getOffset() : 0;
            
            enderecos = dsl.selectFrom(ENDERECO)
                    .where(finalCondition)
                    .limit(limit)
                    .offset(offset)
                    .fetch()
                    .stream()
                    .map(this::fromRepository)
                    .toList();
        } else {
            enderecos = dsl.selectFrom(ENDERECO)
                    .where(finalCondition)
                    .fetch()
                    .stream()
                    .map(this::fromRepository)
                    .toList();
        }
        
        getPersonIdsFromAddresses(enderecos);
        return enderecos;
    }
    
    /**
     * Conta o total de endereços que atendem aos critérios da query
     * (usado para paginação)
     */
    public long count(EnderecoQuery query) {
        List<Condition> conditions = buildConditions(query);
        
        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);
        
        return dsl.selectCount()
            .from(ENDERECO)
            .where(finalCondition)
            .fetchOne(0, long.class);
    }
    
    private List<Condition> buildConditions(EnderecoQuery query) {
        List<Condition> conditions = new ArrayList<>();
        
        if (query.getId() != null) conditions.add(ENDERECO.ID.eq(query.getId()));
        if (query.getCep() != null && !query.getCep().trim().isEmpty()) conditions.add(ENDERECO.CEP.eq(query.getCep()));
        if (query.getLogradouro() != null && !query.getLogradouro().trim().isEmpty()) conditions.add(ENDERECO.LOGRADOURO.likeIgnoreCase("%" + query.getLogradouro() + "%"));
        if (query.getNumero() != null && !query.getNumero().trim().isEmpty()) conditions.add(ENDERECO.NUMERO.eq(query.getNumero()));
        if (query.getComplemento() != null && !query.getComplemento().trim().isEmpty()) conditions.add(ENDERECO.COMPLEMENTO.likeIgnoreCase("%" + query.getComplemento() + "%"));
        
        return conditions;
    }

    private Endereco fromRepository(EnderecoRecord record) {
        if (record == null) return null;
        
        Endereco endereco = new Endereco();
        endereco.setId(record.getId());
        endereco.setCep(record.getCep());
        endereco.setLogradouro(record.getLogradouro());
        endereco.setNumero(record.getNumero());
        endereco.setComplemento(record.getComplemento());
        endereco.setBairro(record.getBairro());
        endereco.setCidade(record.getCidade());
        endereco.setEstado(record.getEstado());
        endereco.setCoordenadas(record.getCoordenadas());
        endereco.setAddedAt(DateTimeHelper.fromDb(record.getAddedAt()));
        endereco.setUpdatedAt(DateTimeHelper.fromDb(record.getUpdatedAt()));
        endereco.setUpdatedByUserId(record.getUpdatedBy());
        
        return endereco;
    }

    /**
     * Popula pessoaIds de cada endereço em uma única query (WHERE endereco_id IN ...),
     * evitando N+1 tanto na listagem de endereços quanto no include de endereço em pessoa.
     */
    public void getPersonIdsFromAddresses(Collection<Endereco> addresses) {
        List<Long> addressIds = addresses.stream()
            .filter(Objects::nonNull)
            .map(Endereco::getId)
            .distinct()
            .toList();
        if (addressIds.isEmpty()) return;

        // A gestação herda o endereço do chefe de família: sem o filtro, o morador a mais
        // denuncia a existência da linha em sigilo para quem não pode vê-la.
        Condition residents = PESSOA.ENDERECO_ID.in(addressIds);
        if (!ConfidentialRows.canSee(ConfidentialAccess.CALLER)) {
            residents = residents.and(ConfidentialRows.notConfidential(PESSOA.CATEGORIA_ID));
        }

        Map<Long, List<Long>> personIdsByAddress = dsl
            .select(PESSOA.ENDERECO_ID, PESSOA.PESSOA_ID)
            .from(PESSOA)
            .where(residents)
            .orderBy(PESSOA.PESSOA_ID.asc())
            .fetchGroups(PESSOA.ENDERECO_ID, PESSOA.PESSOA_ID);

        for (Endereco address : addresses) {
            if (address != null) {
                address.setPessoaIds(
                    personIdsByAddress.getOrDefault(address.getId(), List.of()));
            }
        }
    }

    private static EnderecoRecord toRepository(Endereco endereco) {
        EnderecoRecord record = new EnderecoRecord();
        
        if (endereco.getId() != null) {
            record.setId(endereco.getId());
        }
        record.setCep(endereco.getCep());
        record.setLogradouro(endereco.getLogradouro());
        record.setNumero(endereco.getNumero());
        record.setComplemento(endereco.getComplemento());
        record.setBairro(endereco.getBairro());
        record.setCidade(endereco.getCidade());
        record.setEstado(endereco.getEstado());
        record.setCoordenadas(endereco.getCoordenadas());
        record.setUpdatedBy(endereco.getUpdatedByUserId());
        
        return record;
    }
    
    /**
     * Deleta um endereço
     */
    public void delete(Long id) {
        dsl.deleteFrom(ENDERECO)
            .where(ENDERECO.ID.eq(id))
            .execute();
    }
}

