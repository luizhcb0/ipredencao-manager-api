package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaHistoryRecord;
import org.ipredencao.ipredencao_manager.model.relacionamento_pessoa.RelacionamentoPessoaIds;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.relacionamento_pessoa.RelacionamentoPessoa;
import org.ipredencao.ipredencao_manager.model.TipoRelacionamento;
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
        
        List<Pessoa> pessoas = dsl.selectFrom(PESSOA)
                .where(finalCondition)
                .fetch()
                .stream()
                .map(PessoaRepository::fromRepository)
                .toList();
        
        // Carregar relacionamentos para cada pessoa
        for (Pessoa pessoa : pessoas) {
            List<RelacionamentoPessoaIds> relacionamentos = buscarRelacionamentosPorPessoa(pessoa.getId());
            pessoa.setRelacionamentos(relacionamentos);
        }
        
        return pessoas;
    }

    // CRUD para relacionamentos qualificados
    public RelacionamentoPessoa insertRelationship(Long pessoaId, RelacionamentoPessoaIds relacionamento) {
        PessoaRelacionamentoRecord relationamentoRecord = toRepository(relacionamento);
        relationamentoRecord.setPessoaId(pessoaId);
        PessoaRelacionamentoRecord saved = dsl.insertInto(PESSOA_RELACIONAMENTO)
                .set(relationamentoRecord)
                .returning()
                .fetchOne();
        return mapRelationshipToFull(saved, pessoaId);
    }

    public List<RelacionamentoPessoa> listarRelacionamentosPorPessoa(Long pessoaId) {
        return dsl.selectFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId)
                    .or(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaId)))
                .fetch()
                .stream()
                .map(record -> mapRelationshipToFull(record, pessoaId))
                .toList();
    }
    
    /**
     * Busca todos os relacionamentos de uma pessoa, considerando que ela pode estar
     * tanto na coluna pessoa_id quanto na pessoa_relacionada_id
     */
    private List<RelacionamentoPessoaIds> buscarRelacionamentosPorPessoa(Long pessoaId) {
        return dsl.selectFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId)
                    .or(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaId)))
                .fetch()
                .stream()
                .map(record -> mapRelationship(record, pessoaId))
                .toList();
    }
    
    /**
     * Mapeia um record de relacionamento para RelacionamentoPessoaIds,
     * ajustando a perspectiva para a pessoa especificada
     */
    private RelacionamentoPessoaIds mapRelationship(PessoaRelacionamentoRecord record, Long pessoaId) {
        RelacionamentoPessoaIds rel = new RelacionamentoPessoaIds();
        rel.setId(record.getId());
        rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        
        boolean pessoalPrincipal = record.getPessoaId().equals(pessoaId);
        
        if (pessoalPrincipal) {
            // Pessoa atual é a principal - usar dados diretos
            rel.setPessoaId(record.getPessoaId());
            rel.setPessoaRelacionadaId(record.getPessoaRelacionadaId());
            if (record.getTipoRelacionamento() != null) {
                rel.setTipoRelacionamento(
                    TipoRelacionamento.valueOf(record.getTipoRelacionamento().name())
                );
            }
        } else {
            // Pessoa atual é a relacionada - inverter perspectiva
            rel.setPessoaId(record.getPessoaRelacionadaId()); // Pessoa atual
            rel.setPessoaRelacionadaId(record.getPessoaId()); // Outra pessoa
            if (record.getTipoRelacionamento() != null) {
                TipoRelacionamento tipoOriginal = TipoRelacionamento.valueOf(record.getTipoRelacionamento().name());
                rel.setTipoRelacionamento(inverterTipoRelacionamento(tipoOriginal));
            }
        }
        
        return rel;
    }
    
    /**
     * Mapeia um record de relacionamento para RelacionamentoPessoa (com objetos Pessoa completos),
     * ajustando a perspectiva para a pessoa especificada
     */
    private RelacionamentoPessoa mapRelationshipToFull(PessoaRelacionamentoRecord record, Long pessoaId) {
        RelacionamentoPessoa rel = new RelacionamentoPessoa();
        rel.setId(record.getId());
        rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        
        boolean pessoalPrincipal = record.getPessoaId().equals(pessoaId);
        
        if (pessoalPrincipal) {
            // Pessoa atual é a principal - usar dados diretos
            rel.setPessoa(buscarPessoaPorId(record.getPessoaId()));
            rel.setPessoaRelacionada(buscarPessoaPorId(record.getPessoaRelacionadaId()));
            if (record.getTipoRelacionamento() != null) {
                rel.setTipoRelacionamento(
                    TipoRelacionamento.valueOf(record.getTipoRelacionamento().name())
                );
            }
        } else {
            // Pessoa atual é a relacionada - inverter perspectiva
            rel.setPessoa(buscarPessoaPorId(record.getPessoaRelacionadaId())); // Pessoa atual
            rel.setPessoaRelacionada(buscarPessoaPorId(record.getPessoaId())); // Outra pessoa
            if (record.getTipoRelacionamento() != null) {
                TipoRelacionamento tipoOriginal = TipoRelacionamento.valueOf(record.getTipoRelacionamento().name());
                rel.setTipoRelacionamento(inverterTipoRelacionamento(tipoOriginal));
            }
        }
        
        return rel;
    }
    
    /**
     * Busca uma pessoa por ID (método auxiliar para evitar recursão infinita)
     */
    private Pessoa buscarPessoaPorId(Long id) {
        PessoaRecord record = dsl.selectFrom(PESSOA)
                .where(PESSOA.PESSOA_ID.eq(id))
                .fetchOne();
        return record != null ? fromRepository(record) : null;
    }
    
    /**
     * Inverte o tipo de relacionamento para manter a perspectiva da pessoa atual
     * Ex: se A é FILHO de B, então B é PAI de A
     */
    private TipoRelacionamento inverterTipoRelacionamento(TipoRelacionamento tipo) {
        return switch (tipo) {
            case SEM_RELACIONAMENTO -> TipoRelacionamento.SEM_RELACIONAMENTO; // Sem relacionamento é recíproco
            case CONJUGE -> TipoRelacionamento.CONJUGE; // Cônjuge é recíproco
            case NOIVO -> TipoRelacionamento.NOIVO; // Noivo é recíproco
            case NAMORADO -> TipoRelacionamento.NAMORADO; // Namorado é recíproco
            case FILHO -> TipoRelacionamento.PAI; // Se A é FILHO de B, então B é PAI de A
            case PAI -> TipoRelacionamento.FILHO; // Se A é PAI de B, então B é FILHO de A
            case MAE -> TipoRelacionamento.FILHO; // Se A é MÃE de B, então B é FILHO de A
            case IRMAO -> TipoRelacionamento.IRMAO; // Irmão é recíproco
            case RESPONSAVEL -> TipoRelacionamento.FILHO; // Se A é RESPONSÁVEL de B, então B é FILHO de A
        };
    }
    

    private List<Condition> buildConditions(PessoaQuery query) {
        List<Condition> conditions = new java.util.ArrayList<>();
        
        if (query.getId() != null) conditions.add(PESSOA.PESSOA_ID.eq(query.getId()));
        if (query.getIds() != null && !query.getIds().isEmpty()) conditions.add(PESSOA.PESSOA_ID.in(query.getIds()));
        if (query.getNome() != null && !query.getNome().trim().isEmpty()) conditions.add(PESSOA.NOME.likeIgnoreCase("%" + query.getNome() + "%"));
        if (query.getApelido() != null && !query.getApelido().trim().isEmpty()) conditions.add(PESSOA.APELIDO.likeIgnoreCase("%" + query.getApelido() + "%"));
        if (query.getEmail() != null && !query.getEmail().trim().isEmpty()) conditions.add(PESSOA.EMAIL.eq(query.getEmail()));
        if (query.getTelefone() != null && !query.getTelefone().trim().isEmpty()) conditions.add(PESSOA.TELEFONE.eq(query.getTelefone()));
        if (query.getCpf() != null && !query.getCpf().trim().isEmpty()) conditions.add(PESSOA.CPF.eq(query.getCpf()));
        if (query.getRg() != null && !query.getRg().trim().isEmpty()) conditions.add(PESSOA.RG.eq(query.getRg()));
        if (query.getEstadoCivil() != null) 
            conditions.add(PESSOA.ESTADO_CIVIL.eq(org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil.valueOf(query.getEstadoCivil().name())));
        if (query.getCampus() != null && !query.getCampus().trim().isEmpty()) conditions.add(PESSOA.CAMPUS.eq(query.getCampus()));
        if (query.getRegiao() != null) 
            conditions.add(PESSOA.REGIAO.eq(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(query.getRegiao().name())));
        if (query.getDataNascimentoFrom() != null)
            conditions.add(PESSOA.DATA_NASCIMENTO.greaterOrEqual(DateTimeHelper.toDb(query.getDataNascimentoFrom())));
        if (query.getDataNascimentoTo() != null) 
            conditions.add(PESSOA.DATA_NASCIMENTO.lessOrEqual(DateTimeHelper.toDb(query.getDataNascimentoTo())));
        if (query.getTipoBatismo() != null) 
            conditions.add(PESSOA.TIPO_BATISMO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(query.getTipoBatismo().name())));
        if (query.getSubcategoria() != null) 
            conditions.add(PESSOA.CATEGORIA_ID.eq(query.getSubcategoria().getId()));
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
        p.setEnderecoCep(pessoaRecord.getEnderecoCep());
        p.setEnderecoLogradouro(pessoaRecord.getEnderecoLogradouro());
        p.setEnderecoNumero(pessoaRecord.getEnderecoNumero());
        p.setEnderecoComplemento(pessoaRecord.getEnderecoComplemento());
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
        pessoaRecord.setEnderecoCep(pessoa.getEnderecoCep());
        pessoaRecord.setEnderecoLogradouro(pessoa.getEnderecoLogradouro());
        pessoaRecord.setEnderecoNumero(pessoa.getEnderecoNumero());
        pessoaRecord.setEnderecoComplemento(pessoa.getEnderecoComplemento());
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

    private static PessoaHistoryRecord toHistoryRepository(PessoaRecord pessoaRecord) {
        PessoaHistoryRecord pessoaHistoryRecord = new PessoaHistoryRecord();
        
        // Mapear o pessoa_id (campo obrigatório na tabela history)
        pessoaHistoryRecord.setPessoaId(pessoaRecord.getPessoaId());
        
        pessoaHistoryRecord.setNome(pessoaRecord.getNome());
        pessoaHistoryRecord.setApelido(pessoaRecord.getApelido());
        pessoaHistoryRecord.setEmail(pessoaRecord.getEmail());
        pessoaHistoryRecord.setTelefone(pessoaRecord.getTelefone());
        pessoaHistoryRecord.setCampus(pessoaRecord.getCampus());
        pessoaHistoryRecord.setDataNascimento(pessoaRecord.getDataNascimento());
        pessoaHistoryRecord.setCpf(pessoaRecord.getCpf());
        pessoaHistoryRecord.setRg(pessoaRecord.getRg());
        if (pessoaRecord.getEstadoCivil() != null)
            pessoaHistoryRecord.setEstadoCivil(pessoaRecord.getEstadoCivil());
        pessoaHistoryRecord.setIgrejaAnterior(pessoaRecord.getIgrejaAnterior());
        pessoaHistoryRecord.setSituacaoIgrejaAnterior(pessoaRecord.getSituacaoIgrejaAnterior());
        pessoaHistoryRecord.setTempoNaIgreja(pessoaRecord.getTempoNaIgreja());
        pessoaHistoryRecord.setMotivosParaAdmissao(pessoaRecord.getMotivosParaAdmissao());
        if (pessoaRecord.getTipoBatismo() != null)
            pessoaHistoryRecord.setTipoBatismo(pessoaRecord.getTipoBatismo());
        pessoaHistoryRecord.setDataBatismo(pessoaRecord.getDataBatismo());
        pessoaHistoryRecord.setDataProfissaoDeFe(pessoaRecord.getDataProfissaoDeFe());
        pessoaHistoryRecord.setIgrejaBatismo(pessoaRecord.getIgrejaBatismo());
        pessoaHistoryRecord.setProfissao(pessoaRecord.getProfissao());
        pessoaHistoryRecord.setEmpresa(pessoaRecord.getEmpresa());
        pessoaHistoryRecord.setEnderecoCep(pessoaRecord.getEnderecoCep());
        pessoaHistoryRecord.setEnderecoLogradouro(pessoaRecord.getEnderecoLogradouro());
        pessoaHistoryRecord.setEnderecoNumero(pessoaRecord.getEnderecoNumero());
        pessoaHistoryRecord.setEnderecoComplemento(pessoaRecord.getEnderecoComplemento());
        if (pessoaRecord.getRegiao() != null)
            pessoaHistoryRecord.setRegiao(pessoaRecord.getRegiao());
        if (pessoaRecord.getLatitude() != null) pessoaHistoryRecord.setLatitude(pessoaRecord.getLatitude());
        if (pessoaRecord.getLongitude() != null) pessoaHistoryRecord.setLongitude(pessoaRecord.getLongitude());
        pessoaHistoryRecord.setFotoUrl(pessoaRecord.getFotoUrl());
        if (pessoaRecord.getSexo() != null)
            pessoaHistoryRecord.setSexo(pessoaRecord.getSexo());
        pessoaHistoryRecord.setChefeDeFamilia(pessoaRecord.getChefeDeFamilia());
        pessoaHistoryRecord.setCategoriaId(pessoaRecord.getCategoriaId());

        // Mapear arrays do PostgreSQL
        if (pessoaRecord.getEmailsSecundarios() != null && pessoaRecord.getEmailsSecundarios().length > 0) {
            pessoaHistoryRecord.setEmailsSecundarios(pessoaRecord.getEmailsSecundarios());
        }
        if (pessoaRecord.getTelefonesSecundarios() != null && pessoaRecord.getTelefonesSecundarios().length > 0) {
            pessoaHistoryRecord.setTelefonesSecundarios(pessoaRecord.getTelefonesSecundarios());
        }

        return pessoaHistoryRecord;
    }


    private static PessoaRelacionamentoRecord toRepository(RelacionamentoPessoaIds relacionamento) {
        PessoaRelacionamentoRecord record = new PessoaRelacionamentoRecord();

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