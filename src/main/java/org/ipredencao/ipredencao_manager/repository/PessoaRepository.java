package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaHistoryRecord;
import org.ipredencao.ipredencao_manager.model.relacionamento_pessoa.RelacionamentoPessoaIds;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.relacionamento_pessoa.RelacionamentoPessoa;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA_HISTORY;
import static org.ipredencao.ipredencao_manager.jooq.tables.Pessoa.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.tables.PessoaRelacionamento.PESSOA_RELACIONAMENTO;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRecord;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRelacionamentoRecord;
import java.util.List;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil;
import org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo;
import org.ipredencao.ipredencao_manager.model.PessoaQuery;
import org.jooq.Condition;
import org.jooq.impl.DSL;

@Repository
public class PessoaRepository {
    @Autowired
    private DSLContext dsl;
    
    public Pessoa insert(Pessoa pessoa) {
        PessoaRecord pessoaRecord = toRepository(pessoa);
        
        PessoaRecord saved = dsl.insertInto(PESSOA)
                .set(pessoaRecord)
                .returning()
                .fetchOne();

        insertHistory(saved);

        return fromRepository(saved);
    }

    public Pessoa update(Pessoa pessoa) {
        PessoaRecord pessoaRecord = toRepository(pessoa);
        PessoaRecord updated = dsl.update(PESSOA)
            .set(pessoaRecord)
            .set(PESSOA.UPDATED_AT, DateTimeHelper.toDb(DateTime.now()))
            .where(PESSOA.PESSOA_ID.eq(pessoa.getId()))
            .returning()
            .fetchOne();

        insertHistory(updated);

        return pessoa;
    }

    private PessoaHistoryRecord insertHistory(PessoaRecord pessoaRecord) {
        PessoaHistoryRecord historyRecord = toHistoryRepository(pessoaRecord);
        return dsl.insertInto(PESSOA_HISTORY)
            .set(historyRecord)
            .returning()
            .fetchOne();
    }

    public List<Pessoa> find(PessoaQuery query) {
        List<Condition> conditions = buildConditions(query);
        
        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);
        
        return dsl.selectFrom(PESSOA)
                .where(finalCondition)
                .fetch()
                .stream()
                .map(PessoaRepository::fromRepository)
            .toList();
    }

    // CRUD para relacionamentos qualificados
    public RelacionamentoPessoa insertRelationship(Long pessoaId, RelacionamentoPessoaIds relacionamento) {
        PessoaRelacionamentoRecord relationamentoRecord = toRepository(relacionamento);
        relationamentoRecord.setPessoaId(pessoaId);
        PessoaRelacionamentoRecord saved = dsl.insertInto(PESSOA_RELACIONAMENTO)
                .set(relationamentoRecord)
                .returning()
                .fetchOne();
        return fromRepository(saved);
    }

    public List<RelacionamentoPessoa> listarRelacionamentosPorPessoa(Long pessoaId) {
        return dsl.selectFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId))
                .fetch()
                .stream()
                .map(this::fromRepository)
                .toList();
    }
    

    private List<Condition> buildConditions(PessoaQuery query) {
        List<Condition> conditions = new java.util.ArrayList<>();
        
        query.getId().ifPresent(id -> conditions.add(PESSOA.PESSOA_ID.eq(id)));
        query.getIds().ifPresent(ids -> conditions.add(PESSOA.PESSOA_ID.in(ids)));
        query.getNome().ifPresent(nome -> conditions.add(PESSOA.NOME.like("%" + nome + "%")));
        query.getApelido().ifPresent(apelido -> conditions.add(PESSOA.APELIDO.like("%" + apelido + "%")));
        query.getEmail().ifPresent(email -> conditions.add(PESSOA.EMAIL.eq(email)));
        query.getTelefone().ifPresent(telefone -> conditions.add(PESSOA.TELEFONE.eq(telefone)));
        query.getCpf().ifPresent(cpf -> conditions.add(PESSOA.CPF.eq(cpf)));
        query.getRg().ifPresent(rg -> conditions.add(PESSOA.RG.eq(rg)));
        query.getEstadoCivil().ifPresent(estadoCivil -> 
            conditions.add(PESSOA.ESTADO_CIVIL.eq(org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil.valueOf(estadoCivil.name()))));
        query.getCampus().ifPresent(campus -> conditions.add(PESSOA.CAMPUS.eq(campus)));
        query.getRegiao().ifPresent(regiao -> 
            conditions.add(PESSOA.REGIAO.eq(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(regiao.name()))));
        query.getDataNascimentoFrom().ifPresent(from ->
            conditions.add(PESSOA.DATA_NASCIMENTO.greaterOrEqual(DateTimeHelper.toDb(from))));
        query.getDataNascimentoTo().ifPresent(to -> 
            conditions.add(PESSOA.DATA_NASCIMENTO.lessOrEqual(DateTimeHelper.toDb(to))));
        query.getTipoBatismo().ifPresent(tipoBatismo -> 
            conditions.add(PESSOA.TIPO_BATISMO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(tipoBatismo.name()))));
        query.getSubcategoria().ifPresent(subcategoria -> 
            conditions.add(PESSOA.CATEGORIA_ID.eq(subcategoria.getId())));
        return conditions;
    }

    private static Pessoa fromRepository(PessoaRecord pessoaRecord) {
        if (pessoaRecord == null) return null;
        Pessoa p = new Pessoa();
        p.setId(pessoaRecord.getPessoaId());
        p.setNome(pessoaRecord.getNome());
        p.setApelido(pessoaRecord.getApelido());
        p.setEmail(pessoaRecord.getEmail());
        p.setTelefone(pessoaRecord.getTelefone());
        p.setCampus(pessoaRecord.getCampus());
        p.setDataNascimento(DateTimeHelper.fromDb(pessoaRecord.getDataNascimento()));
        p.setCpf(pessoaRecord.getCpf());
        p.setRg(pessoaRecord.getRg());
        if (pessoaRecord.getEstadoCivil() != null)
            p.setEstadoCivil(org.ipredencao.ipredencao_manager.model.EstadoCivil.valueOf(pessoaRecord.getEstadoCivil().name()));
        p.setIgrejaAnterior(pessoaRecord.getIgrejaAnterior());
        p.setSituacaoIgrejaAnterior(pessoaRecord.getSituacaoIgrejaAnterior());
        p.setTempoNaIgreja(pessoaRecord.getTempoNaIgreja());
        p.setMotivosParaAdmissao(pessoaRecord.getMotivosParaAdmissao());
        if (pessoaRecord.getTipoBatismo() != null)
            p.setTipoBatismo(org.ipredencao.ipredencao_manager.model.TipoBatismo.valueOf(pessoaRecord.getTipoBatismo().name()));
        p.setDataBatismo(DateTimeHelper.fromDb(pessoaRecord.getDataBatismo()));
        p.setDataProfissaoDeFe(DateTimeHelper.fromDb(pessoaRecord.getDataProfissaoDeFe()));
        p.setIgrejaBatismo(pessoaRecord.getIgrejaBatismo());
        p.setProfissao(pessoaRecord.getProfissao());
        p.setEmpresa(pessoaRecord.getEmpresa());
        p.setEndereco(pessoaRecord.getEndereco());
        p.setCep(pessoaRecord.getCep());
        if (pessoaRecord.getRegiao() != null)
            p.setRegiao(org.ipredencao.ipredencao_manager.model.Regiao.valueOf(pessoaRecord.getRegiao().name()));
        p.setLatitude(pessoaRecord.getLatitude() != null ? pessoaRecord.getLatitude().doubleValue() : null);
        p.setLongitude(pessoaRecord.getLongitude() != null ? pessoaRecord.getLongitude().doubleValue() : null);
        p.setFotoUrl(pessoaRecord.getFotoUrl());
        if (pessoaRecord.getSexo() != null)
            p.setSexo(org.ipredencao.ipredencao_manager.model.Sexo.valueOf(pessoaRecord.getSexo().name()));
        p.setChefeDeFamiliaId(pessoaRecord.getChefeDeFamilia());
        if (pessoaRecord.getCategoriaId() != null)
            p.setSubcategoria(org.ipredencao.ipredencao_manager.model.SubcategoriaEnum.fromId(pessoaRecord.getCategoriaId()));
        
        // Converter arrays do PostgreSQL para List<String>
        if (pessoaRecord.getEmailsSecundarios() != null) {
            p.setEmailsSecundarios(java.util.Arrays.asList(pessoaRecord.getEmailsSecundarios()));
        }
        if (pessoaRecord.getTelefonesSecundarios() != null) {
            p.setTelefonesSecundarios(java.util.Arrays.asList(pessoaRecord.getTelefonesSecundarios()));
        }
        
        return p;
    }

    private static PessoaRecord toRepository(Pessoa pessoa) {
        PessoaRecord pessoaRecord = new PessoaRecord();
        pessoaRecord.setNome(pessoa.getNome());
        pessoaRecord.setApelido(pessoa.getApelido());
        pessoaRecord.setEmail(pessoa.getEmail());
        pessoaRecord.setTelefone(pessoa.getTelefone());
        pessoaRecord.setCampus(pessoa.getCampus());
        pessoaRecord.setDataNascimento(DateTimeHelper.toDb(pessoa.getDataNascimento()));
        pessoaRecord.setCpf(pessoa.getCpf());
        pessoaRecord.setRg(pessoa.getRg());
        if (pessoa.getEstadoCivil() != null)
            pessoaRecord.setEstadoCivil(EstadoCivil.valueOf(pessoa.getEstadoCivil().name()));
        pessoaRecord.setIgrejaAnterior(pessoa.getIgrejaAnterior());
        pessoaRecord.setSituacaoIgrejaAnterior(pessoa.getSituacaoIgrejaAnterior());
        pessoaRecord.setTempoNaIgreja(pessoa.getTempoNaIgreja());
        pessoaRecord.setMotivosParaAdmissao(pessoa.getMotivosParaAdmissao());
        if (pessoa.getTipoBatismo() != null)
            pessoaRecord.setTipoBatismo(TipoBatismo.valueOf(pessoa.getTipoBatismo().name()));
        pessoaRecord.setDataBatismo(DateTimeHelper.toDb(pessoa.getDataBatismo()));
        pessoaRecord.setDataProfissaoDeFe(DateTimeHelper.toDb(pessoa.getDataProfissaoDeFe()));
        pessoaRecord.setIgrejaBatismo(pessoa.getIgrejaBatismo());
        pessoaRecord.setProfissao(pessoa.getProfissao());
        pessoaRecord.setEmpresa(pessoa.getEmpresa());
        pessoaRecord.setEndereco(pessoa.getEndereco());
        pessoaRecord.setCep(pessoa.getCep());
        if (pessoa.getRegiao() != null)
            pessoaRecord.setRegiao(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(pessoa.getRegiao().name()));
        if (pessoa.getLatitude() != null) pessoaRecord.setLatitude(java.math.BigDecimal.valueOf(pessoa.getLatitude()));
        if (pessoa.getLongitude() != null) pessoaRecord.setLongitude(java.math.BigDecimal.valueOf(pessoa.getLongitude()));
        pessoaRecord.setFotoUrl(pessoa.getFotoUrl());
        if (pessoa.getSexo() != null)
            pessoaRecord.setSexo(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(pessoa.getSexo().name()));
        pessoaRecord.setChefeDeFamilia(pessoa.getChefeDeFamiliaId());
        if (pessoa.getSubcategoria() != null) {
            pessoaRecord.setCategoriaId(pessoa.getSubcategoria().getId());
        }
        
        // Converter List<String> para arrays do PostgreSQL
        if (pessoa.getEmailsSecundarios() != null && !pessoa.getEmailsSecundarios().isEmpty()) {
            pessoaRecord.setEmailsSecundarios(pessoa.getEmailsSecundarios().toArray(new String[0]));
        }
        if (pessoa.getTelefonesSecundarios() != null && !pessoa.getTelefonesSecundarios().isEmpty()) {
            pessoaRecord.setTelefonesSecundarios(pessoa.getTelefonesSecundarios().toArray(new String[0]));
        }
        
        return pessoaRecord;
    }

    private static PessoaHistoryRecord toHistoryRepository(PessoaRecord pessoa) {
        PessoaHistoryRecord pessoaHistoryRecord = new PessoaHistoryRecord();
        
        // Mapear o pessoa_id (campo obrigatório na tabela history)
        pessoaHistoryRecord.setPessoaId(pessoa.getPessoaId());
        
        pessoaHistoryRecord.setNome(pessoa.getNome());
        pessoaHistoryRecord.setApelido(pessoa.getApelido());
        pessoaHistoryRecord.setEmail(pessoa.getEmail());
        pessoaHistoryRecord.setTelefone(pessoa.getTelefone());
        pessoaHistoryRecord.setCampus(pessoa.getCampus());
        pessoaHistoryRecord.setDataNascimento(pessoa.getDataNascimento());
        pessoaHistoryRecord.setCpf(pessoa.getCpf());
        pessoaHistoryRecord.setRg(pessoa.getRg());
        if (pessoa.getEstadoCivil() != null)
            pessoaHistoryRecord.setEstadoCivil(EstadoCivil.valueOf(pessoa.getEstadoCivil().name()));
        pessoaHistoryRecord.setIgrejaAnterior(pessoa.getIgrejaAnterior());
        pessoaHistoryRecord.setSituacaoIgrejaAnterior(pessoa.getSituacaoIgrejaAnterior());
        pessoaHistoryRecord.setTempoNaIgreja(pessoa.getTempoNaIgreja());
        pessoaHistoryRecord.setMotivosParaAdmissao(pessoa.getMotivosParaAdmissao());
        if (pessoa.getTipoBatismo() != null)
            pessoaHistoryRecord.setTipoBatismo(TipoBatismo.valueOf(pessoa.getTipoBatismo().name()));
        pessoaHistoryRecord.setDataBatismo(pessoa.getDataBatismo());
        pessoaHistoryRecord.setDataProfissaoDeFe(pessoa.getDataProfissaoDeFe());
        pessoaHistoryRecord.setIgrejaBatismo(pessoa.getIgrejaBatismo());
        pessoaHistoryRecord.setProfissao(pessoa.getProfissao());
        pessoaHistoryRecord.setEmpresa(pessoa.getEmpresa());
        pessoaHistoryRecord.setEndereco(pessoa.getEndereco());
        pessoaHistoryRecord.setCep(pessoa.getCep());
        if (pessoa.getRegiao() != null)
            pessoaHistoryRecord.setRegiao(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(pessoa.getRegiao().name()));
        if (pessoa.getLatitude() != null) pessoaHistoryRecord.setLatitude(pessoa.getLatitude());
        if (pessoa.getLongitude() != null) pessoaHistoryRecord.setLongitude(pessoa.getLongitude());
        pessoaHistoryRecord.setFotoUrl(pessoa.getFotoUrl());
        if (pessoa.getSexo() != null)
            pessoaHistoryRecord.setSexo(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(pessoa.getSexo().name()));
        pessoaHistoryRecord.setChefeDeFamilia(pessoa.getChefeDeFamilia());
        pessoaHistoryRecord.setCategoriaId(pessoa.getCategoriaId());

        // Mapear arrays do PostgreSQL
        if (pessoa.getEmailsSecundarios() != null && pessoa.getEmailsSecundarios().length > 0) {
            pessoaHistoryRecord.setEmailsSecundarios(pessoa.getEmailsSecundarios());
        }
        if (pessoa.getTelefonesSecundarios() != null && pessoa.getTelefonesSecundarios().length > 0) {
            pessoaHistoryRecord.setTelefonesSecundarios(pessoa.getTelefonesSecundarios());
        }

        return pessoaHistoryRecord;
    }

    private RelacionamentoPessoa fromRepository(PessoaRelacionamentoRecord record) {
        if (record == null) return null;
        RelacionamentoPessoa rel = new RelacionamentoPessoa();
        rel.setId(record.getId());
        
        // Buscar pessoa principal completa da tabela pessoa
        if (record.getPessoaId() != null) {
            List<Pessoa> pessoaPrincipalResult = find(PessoaQuery.builder().id(record.getPessoaId()).build());
            if (!pessoaPrincipalResult.isEmpty()) {
                rel.setPessoa(pessoaPrincipalResult.getFirst());
            }
        }
        
        // Buscar pessoa relacionada completa da tabela pessoa
        if (record.getPessoaRelacionadaId() != null) {
            List<Pessoa> pessoaRelacionadaResult = find(PessoaQuery.builder().id(record.getPessoaRelacionadaId()).build());
            if (!pessoaRelacionadaResult.isEmpty()) {
                rel.setPessoaRelacionada(pessoaRelacionadaResult.getFirst());
            }
        }
        
        if (record.getTipoRelacionamento() != null)
            rel.setTipoRelacionamento(
                org.ipredencao.ipredencao_manager.model.TipoRelacionamento.valueOf(
                    record.getTipoRelacionamento().name()
                )
            );
        rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        return rel;
    }

    private static PessoaRelacionamentoRecord toRepository(RelacionamentoPessoaIds relacionamento) {
        PessoaRelacionamentoRecord record = new PessoaRelacionamentoRecord();
        record.setId(relacionamento.getId());

        record.setPessoaId(relacionamento.getPessoaId());
        record.setPessoaRelacionadaId(relacionamento.getPessoaRelacionadaId());
        if (relacionamento.getTipoRelacionamento() != null)
            record.setTipoRelacionamento(
                org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(
                    relacionamento.getTipoRelacionamento().name()
                )
            );
        record.setInicioRelacionamento(DateTimeHelper.toDb(relacionamento.getInicioRelacionamento()));
        return record;
    }
} 