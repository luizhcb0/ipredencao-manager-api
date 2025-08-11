package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import static org.ipredencao.ipredencao_manager.jooq.tables.FormularioPessoa.FORMULARIO_PESSOA;

import org.ipredencao.ipredencao_manager.jooq.tables.records.FormularioPessoaRecord;

@Repository
public class FormularioPessoaRepository {
    @Autowired
    private DSLContext dsl;

    public FormularioPessoa insert(FormularioPessoa formulario) {
        FormularioPessoaRecord record = toRepository(formulario);
        FormularioPessoaRecord saved = dsl.insertInto(FORMULARIO_PESSOA)
                .set(record)
                .returning()
                .fetchOne();
        return fromRepository(saved);
    }

    public FormularioPessoa update(FormularioPessoa formulario) {
        FormularioPessoaRecord record = toRepository(formulario);
        FormularioPessoaRecord updated = dsl.update(FORMULARIO_PESSOA)
                .set(record)
                .set(FORMULARIO_PESSOA.UPDATED_AT, DateTimeHelper.toDb(DateTime.now()))
                .where(FORMULARIO_PESSOA.FORMULARIO_PESSOA_ID.eq(formulario.getId()))
                .returning()
                .fetchOne();
        return fromRepository(updated);
    }

    public List<FormularioPessoa> findAll() {
        return dsl.selectFrom(FORMULARIO_PESSOA)
                .fetch()
                .stream()
                .map(FormularioPessoaRepository::fromRepository)
                .collect(Collectors.toList());
    }

    public List<FormularioPessoa> find(FormularioPessoaQuery query) {
        List<Condition> conditions = buildConditions(query);

        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);

        return dsl.selectFrom(FORMULARIO_PESSOA)
            .where(finalCondition)
            .fetch()
            .stream()
            .map(FormularioPessoaRepository::fromRepository)
            .toList();
    }

    private List<Condition> buildConditions(FormularioPessoaQuery query) {
        List<Condition> conditions = new java.util.ArrayList<>();

        if (query.getId() != null) conditions.add(FORMULARIO_PESSOA.FORMULARIO_PESSOA_ID.eq(query.getId()));
        if (query.getIds() != null && !query.getIds().isEmpty()) conditions.add(FORMULARIO_PESSOA.FORMULARIO_PESSOA_ID.in(query.getIds()));
        if (query.getNome() != null && !query.getNome().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.NOME.like("%" + query.getNome() + "%"));
        if (query.getApelido() != null && !query.getApelido().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.APELIDO.like("%" + query.getApelido() + "%"));
        if (query.getEmail() != null && !query.getEmail().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.EMAIL.eq(query.getEmail()));
        if (query.getTelefone() != null && !query.getTelefone().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.TELEFONE.eq(query.getTelefone()));
        if (query.getCpf() != null && !query.getCpf().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.CPF.eq(query.getCpf()));
        if (query.getRg() != null && !query.getRg().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.RG.eq(query.getRg()));
        if (query.getEstadoCivil() != null)
            conditions.add(FORMULARIO_PESSOA.ESTADO_CIVIL.eq(org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil.valueOf(query.getEstadoCivil().name())));
        if (query.getCampus() != null && !query.getCampus().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.CAMPUS.eq(query.getCampus()));
        if (query.getRegiao() != null)
            conditions.add(FORMULARIO_PESSOA.REGIAO.eq(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(query.getRegiao().name())));
        if (query.getDataNascimentoFrom() != null)
            conditions.add(FORMULARIO_PESSOA.DATA_NASCIMENTO.greaterOrEqual(DateTimeHelper.toDb(query.getDataNascimentoFrom())));
        if (query.getDataNascimentoTo() != null)
            conditions.add(FORMULARIO_PESSOA.DATA_NASCIMENTO.lessOrEqual(DateTimeHelper.toDb(query.getDataNascimentoTo())));
        if (query.getTipoBatismo() != null)
            conditions.add(FORMULARIO_PESSOA.TIPO_BATISMO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(query.getTipoBatismo().name())));
        if (query.getSubcategoria() != null)
            conditions.add(FORMULARIO_PESSOA.CATEGORIA_ID.eq(query.getSubcategoria().getId()));
        return conditions;
    }

    private static FormularioPessoa fromRepository(FormularioPessoaRecord record) {
        if (record == null) return null;
        FormularioPessoa f = new FormularioPessoa();
        f.setId(record.getFormularioPessoaId());
        f.setNome(record.getNome());
        f.setApelido(record.getApelido());
        f.setEmail(record.getEmail());
        f.setTelefone(record.getTelefone());
        f.setCampus(record.getCampus());
        f.setDataNascimento(DateTimeHelper.fromDb(record.getDataNascimento()));
        f.setCpf(record.getCpf());
        f.setRg(record.getRg());
        if (record.getEstadoCivil() != null)
            f.setEstadoCivil(org.ipredencao.ipredencao_manager.model.EstadoCivil.valueOf(record.getEstadoCivil().name()));
        f.setIgrejaAnterior(record.getIgrejaAnterior());
        f.setSituacaoIgrejaAnterior(record.getSituacaoIgrejaAnterior());
        f.setTempoNaIgreja(record.getTempoNaIgreja());
        f.setMotivosParaAdmissao(record.getMotivosParaAdmissao());
        if (record.getTipoBatismo() != null)
            f.setTipoBatismo(org.ipredencao.ipredencao_manager.model.TipoBatismo.valueOf(record.getTipoBatismo().name()));
        f.setDataBatismo(DateTimeHelper.fromDb(record.getDataBatismo()));
        f.setDataProfissaoDeFe(DateTimeHelper.fromDb(record.getDataProfissaoDeFe()));
        f.setIgrejaBatismo(record.getIgrejaBatismo());
        f.setProfissao(record.getProfissao());
        f.setEmpresa(record.getEmpresa());
        f.setEndereco(record.getEndereco());
        f.setCep(record.getCep());
        if (record.getRegiao() != null)
            f.setRegiao(org.ipredencao.ipredencao_manager.model.Regiao.valueOf(record.getRegiao().name()));
        f.setLatitude(record.getLatitude() != null ? record.getLatitude().doubleValue() : null);
        f.setLongitude(record.getLongitude() != null ? record.getLongitude().doubleValue() : null);
        f.setFotoUrl(record.getFotoUrl());
        if (record.getSexo() != null)
            f.setSexo(org.ipredencao.ipredencao_manager.model.Sexo.valueOf(record.getSexo().name()));
        // chefeDeFamilia: no formulário é String, na model é Long. Não mapeia diretamente.
        f.setPropagarEnderecoChefeFamilia(record.getPropagarEnderecoChefeFamilia());
        f.setPessoaId(record.getPessoaId());
        if (record.getCategoriaId() != null)
            f.setSubcategoria(org.ipredencao.ipredencao_manager.model.SubcategoriaEnum.fromId(record.getCategoriaId()));
        if (record.getEmailsSecundarios() != null) {
            f.setEmailsSecundarios(java.util.Arrays.asList(record.getEmailsSecundarios()));
        }
        if (record.getTelefonesSecundarios() != null) {
            f.setTelefonesSecundarios(java.util.Arrays.asList(record.getTelefonesSecundarios()));
        }
        // Campos extras do formulário
        f.setNomePessoaRelacionada(record.getNomePessoaRelacionada());
        f.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        f.setNomePai(record.getNomePai());
        f.setNomeMae(record.getNomeMae());
        f.setNomeFilhos(record.getNomeFilhos() != null ? java.util.Arrays.asList(record.getNomeFilhos()) : null);
        if (record.getStatus() != null)
            f.setStatus(org.ipredencao.ipredencao_manager.model.FormPessoaStatus.valueOf(record.getStatus().name()));
        return f;
    }

    private static FormularioPessoaRecord toRepository(FormularioPessoa f) {
        FormularioPessoaRecord record = new FormularioPessoaRecord();
        if (f.getId() != null) record.setFormularioPessoaId(f.getId());
        record.setNome(f.getNome());
        record.setApelido(f.getApelido());
        record.setEmail(f.getEmail());
        record.setTelefone(f.getTelefone());
        record.setCampus(f.getCampus());
        record.setDataNascimento(DateTimeHelper.toDb(f.getDataNascimento()));
        record.setCpf(f.getCpf());
        record.setRg(f.getRg());
        if (f.getEstadoCivil() != null)
            record.setEstadoCivil(org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil.valueOf(f.getEstadoCivil().name()));
        record.setIgrejaAnterior(f.getIgrejaAnterior());
        record.setSituacaoIgrejaAnterior(f.getSituacaoIgrejaAnterior());
        record.setTempoNaIgreja(f.getTempoNaIgreja());
        record.setMotivosParaAdmissao(f.getMotivosParaAdmissao());
        if (f.getTipoBatismo() != null)
            record.setTipoBatismo(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(f.getTipoBatismo().name()));
        record.setDataBatismo(DateTimeHelper.toDb(f.getDataBatismo()));
        record.setDataProfissaoDeFe(DateTimeHelper.toDb(f.getDataProfissaoDeFe()));
        record.setIgrejaBatismo(f.getIgrejaBatismo());
        record.setProfissao(f.getProfissao());
        record.setEmpresa(f.getEmpresa());
        record.setEndereco(f.getEndereco());
        record.setCep(f.getCep());
        if (f.getRegiao() != null)
            record.setRegiao(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(f.getRegiao().name()));
        if (f.getLatitude() != null) record.setLatitude(java.math.BigDecimal.valueOf(f.getLatitude()));
        if (f.getLongitude() != null) record.setLongitude(java.math.BigDecimal.valueOf(f.getLongitude()));
        record.setFotoUrl(f.getFotoUrl());
        if (f.getSexo() != null)
            record.setSexo(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(f.getSexo().name()));
        // chefeDeFamilia: no formulário é String, na model é Long. Não mapeia diretamente.
        record.setPropagarEnderecoChefeFamilia(f.getPropagarEnderecoChefeFamilia());
        record.setPessoaId(f.getPessoaId());
        if (f.getSubcategoria() != null) {
            record.setCategoriaId(f.getSubcategoria().getId());
        }
        if (f.getEmailsSecundarios() != null && !f.getEmailsSecundarios().isEmpty()) {
            record.setEmailsSecundarios(f.getEmailsSecundarios().toArray(new String[0]));
        }
        if (f.getTelefonesSecundarios() != null && !f.getTelefonesSecundarios().isEmpty()) {
            record.setTelefonesSecundarios(f.getTelefonesSecundarios().toArray(new String[0]));
        }
        // Campos extras do formulário
        record.setNomePessoaRelacionada(f.getNomePessoaRelacionada());
        record.setInicioRelacionamento(DateTimeHelper.toDb(f.getInicioRelacionamento()));
        record.setNomePai(f.getNomePai());
        record.setNomeMae(f.getNomeMae());
        if (f.getNomeFilhos() != null)
            record.setNomeFilhos(f.getNomeFilhos().toArray(new String[0]));
        if (f.getStatus() != null)
            record.setStatus(org.ipredencao.ipredencao_manager.jooq.enums.FormPessoaStatus.valueOf(f.getStatus().name()));
        return record;
    }
} 