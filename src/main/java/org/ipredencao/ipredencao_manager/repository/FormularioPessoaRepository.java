package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.FormPessoaStatus;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.Regiao;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
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

        // Montar query com ou sem paginação
        if (query.getPagination() != null) {
            int limit = query.getPagination().getLimit() != null ? query.getPagination().getLimit() : Integer.MAX_VALUE;
            int offset = query.getPagination().getOffset() != null ? query.getPagination().getOffset() : 0;
            
            return dsl.selectFrom(FORMULARIO_PESSOA)
                    .where(finalCondition)
                    .limit(limit)
                    .offset(offset)
                    .fetch()
                    .stream()
                    .map(FormularioPessoaRepository::fromRepository)
                    .toList();
        } else {
            return dsl.selectFrom(FORMULARIO_PESSOA)
                    .where(finalCondition)
                    .fetch()
                    .stream()
                    .map(FormularioPessoaRepository::fromRepository)
                    .toList();
        }
    }
    
    /**
     * Conta o total de formulários que atendem aos critérios da query
     * (usado para paginação)
     */
    public long count(FormularioPessoaQuery query) {
        List<Condition> conditions = buildConditions(query);

        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);

        return dsl.selectCount()
            .from(FORMULARIO_PESSOA)
            .where(finalCondition)
            .fetchOne(0, long.class);
    }

    private List<Condition> buildConditions(FormularioPessoaQuery query) {
        List<Condition> conditions = new ArrayList<>();

        if (query.getId() != null) conditions.add(FORMULARIO_PESSOA.FORMULARIO_PESSOA_ID.eq(query.getId()));
        if (query.getIds() != null && !query.getIds().isEmpty()) conditions.add(FORMULARIO_PESSOA.FORMULARIO_PESSOA_ID.in(query.getIds()));
        if (query.getNome() != null && !query.getNome().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.NOME.likeIgnoreCase("%" + query.getNome() + "%"));
        if (query.getApelido() != null && !query.getApelido().trim().isEmpty()) conditions.add(FORMULARIO_PESSOA.APELIDO.likeIgnoreCase("%" + query.getApelido() + "%"));
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
        if (query.getCategoria() != null)
            conditions.add(FORMULARIO_PESSOA.CATEGORIA_ID.eq(query.getCategoria().getId()));
        if (query.getStatus() != null)
            conditions.add(FORMULARIO_PESSOA.STATUS.eq(FormPessoaStatus.valueOf(query.getStatus().name())));
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
            f.setEstadoCivil(EstadoCivil.valueOf(record.getEstadoCivil().name()));
        f.setIgrejaAnterior(record.getIgrejaAnterior());
        f.setMotivosParaAdmissao(record.getMotivosParaAdmissao());
        if (record.getTipoBatismo() != null)
            f.setTipoBatismo(TipoBatismo.valueOf(record.getTipoBatismo().name()));
        f.setDataBatismo(DateTimeHelper.fromDb(record.getDataBatismo()));
        f.setDataProfissaoDeFe(DateTimeHelper.fromDb(record.getDataProfissaoDeFe()));
        f.setIgrejaBatismo(record.getIgrejaBatismo());
        
        // Converter arrays do PostgreSQL para List
        if (record.getProfissao() != null && record.getProfissao().length > 0) {
            f.setProfissao(Arrays.asList(record.getProfissao()));
        }
        if (record.getEmpresa() != null && record.getEmpresa().length > 0) {
            f.setEmpresa(Arrays.asList(record.getEmpresa()));
        }
        
        f.setEnderecoCep(record.getEnderecoCep());
        f.setEnderecoLogradouro(record.getEnderecoLogradouro());
        f.setEnderecoNumero(record.getEnderecoNumero());
        f.setEnderecoComplemento(record.getEnderecoComplemento());
        if (record.getRegiao() != null)
            f.setRegiao(Regiao.valueOf(record.getRegiao().name()));
        f.setFotoUrl(record.getFotoUrl());
        if (record.getSexo() != null)
            f.setSexo(Sexo.valueOf(record.getSexo().name()));
        f.setChefeDeFamilia(record.getChefeDeFamilia());
        f.setPropagarEnderecoChefeFamilia(record.getPropagarEnderecoChefeFamilia());
        f.setPessoaId(record.getPessoaId());
        if (record.getCategoriaId() != null)
            f.setCategoria(CategoriaEnum.fromId(record.getCategoriaId()));
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
            f.setStatus(org.ipredencao.ipredencao_manager.model.pessoa.FormPessoaStatus.valueOf(record.getStatus().name()));
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
        record.setMotivosParaAdmissao(f.getMotivosParaAdmissao());
        if (f.getTipoBatismo() != null)
            record.setTipoBatismo(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(f.getTipoBatismo().name()));
        record.setDataBatismo(DateTimeHelper.toDb(f.getDataBatismo()));
        record.setDataProfissaoDeFe(DateTimeHelper.toDb(f.getDataProfissaoDeFe()));
        record.setIgrejaBatismo(f.getIgrejaBatismo());
        
        // Converter List para arrays do PostgreSQL
        if (f.getProfissao() != null && !f.getProfissao().isEmpty()) {
            record.setProfissao(f.getProfissao().toArray(new String[0]));
        }
        if (f.getEmpresa() != null && !f.getEmpresa().isEmpty()) {
            record.setEmpresa(f.getEmpresa().toArray(new String[0]));
        }
        
        record.setEnderecoCep(f.getEnderecoCep());
        record.setEnderecoLogradouro(f.getEnderecoLogradouro());
        record.setEnderecoNumero(f.getEnderecoNumero());
        record.setEnderecoComplemento(f.getEnderecoComplemento());
        if (f.getRegiao() != null)
            record.setRegiao(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(f.getRegiao().name()));
        record.setFotoUrl(f.getFotoUrl());
        if (f.getSexo() != null)
            record.setSexo(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(f.getSexo().name()));
        record.setChefeDeFamilia(f.getChefeDeFamilia());
        record.setPropagarEnderecoChefeFamilia(f.getPropagarEnderecoChefeFamilia());
        record.setPessoaId(f.getPessoaId());
        if (f.getCategoria() != null) {
            record.setCategoriaId(f.getCategoria().getId());
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