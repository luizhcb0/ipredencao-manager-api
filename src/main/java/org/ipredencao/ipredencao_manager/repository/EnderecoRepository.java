package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import static org.ipredencao.ipredencao_manager.jooq.tables.Endereco.ENDERECO;
import org.ipredencao.ipredencao_manager.jooq.tables.records.EnderecoRecord;
import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import java.util.ArrayList;
import java.util.List;
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
        
        return fromRepository(saved);
    }

    public Endereco update(Endereco endereco) {
        EnderecoRecord record = toRepository(endereco);
        
        EnderecoRecord updated = dsl.update(ENDERECO)
                .set(record)
                .where(ENDERECO.ID.eq(endereco.getId()))
                .returning()
                .fetchOne();
        
        return fromRepository(updated);
    }

    public Endereco findById(Long id) {
        if (id == null) return null;
        
        EnderecoRecord record = dsl.selectFrom(ENDERECO)
                .where(ENDERECO.ID.eq(id))
                .fetchOne();
        
        return fromRepository(record);
    }

    public List<Endereco> find(EnderecoQuery query) {
        List<Condition> conditions = new ArrayList<>();
        
        if (query.getId() != null) conditions.add(ENDERECO.ID.eq(query.getId()));
        if (query.getCep() != null && !query.getCep().trim().isEmpty()) conditions.add(ENDERECO.CEP.eq(query.getCep()));
        if (query.getLogradouro() != null && !query.getLogradouro().trim().isEmpty()) conditions.add(ENDERECO.LOGRADOURO.likeIgnoreCase("%" + query.getLogradouro() + "%"));
        if (query.getNumero() != null && !query.getNumero().trim().isEmpty()) conditions.add(ENDERECO.NUMERO.eq(query.getNumero()));
        if (query.getComplemento() != null && !query.getComplemento().trim().isEmpty()) conditions.add(ENDERECO.COMPLEMENTO.likeIgnoreCase("%" + query.getComplemento() + "%"));
        
        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);
        
        List<Endereco> addresses = dsl.selectFrom(ENDERECO)
                .where(finalCondition)
                .fetch()
                .stream()
                .map(EnderecoRepository::fromRepository)
                .toList();
        
        return addresses;
    }

    private static Endereco fromRepository(EnderecoRecord record) {
        if (record == null) return null;
        
        Endereco endereco = new Endereco();
        endereco.setId(record.getId());
        endereco.setCep(record.getCep());
        endereco.setLogradouro(record.getLogradouro());
        endereco.setNumero(record.getNumero());
        endereco.setComplemento(record.getComplemento());
        endereco.setAddedAt(DateTimeHelper.fromDb(record.getAddedAt()));
        endereco.setUpdatedAt(DateTimeHelper.fromDb(record.getUpdatedAt()));
        endereco.setUpdatedByUserId(record.getUpdatedBy());
        
        return endereco;
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
        record.setUpdatedBy(endereco.getUpdatedByUserId());
        
        return record;
    }
}

