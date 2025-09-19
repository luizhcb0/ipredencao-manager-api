package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaHistoryRecord;
import org.ipredencao.ipredencao_manager.model.pessoa.Regiao;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistory;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistoryChange;
import java.util.ArrayList;
import java.util.Arrays;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA_HISTORY;
import static org.ipredencao.ipredencao_manager.jooq.tables.Pessoa.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.tables.PessoaRelacionamento.PESSOA_RELACIONAMENTO;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRecord;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRelacionamentoRecord;
import java.util.List;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil;
import org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.jooq.Condition;
import org.jooq.impl.DSL;

@Repository
public class PessoaRepository {
    @Autowired
    private DSLContext dsl;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
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

    public void deleteRelationship(Long pessoaId, Long pessoaRelacionadaId, TipoRelacionamento tipoRelacionamento) {
        // Tentar deletar na forma direta (pessoaId -> pessoaRelacionadaId)
        int deleted = dsl.deleteFrom(PESSOA_RELACIONAMENTO)
            .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId)
                .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaRelacionadaId))
                .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(tipoRelacionamento.name()))))
            .execute();
        
        // Se não encontrou na forma direta, tentar na forma invertida
        if (deleted == 0) {
            TipoRelacionamento tipoInvertido = inverterTipoRelacionamento(tipoRelacionamento);
            dsl.deleteFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaRelacionadaId)
                    .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaId))
                    .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(tipoInvertido.name()))))
                .execute();
        }
    }
    
    public void updateRelationship(Long pessoaId, Relacionamento existingRel, Relacionamento newRel) {
        // Tentar atualizar na forma direta (pessoaId -> pessoaRelacionadaId)
        int updated = dsl.update(PESSOA_RELACIONAMENTO)
            .set(PESSOA_RELACIONAMENTO.INICIO_RELACIONAMENTO, DateTimeHelper.toDb(newRel.getInicioRelacionamento()))
            .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId)
                .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(existingRel.getPessoaRelacionadaId()))
                .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(existingRel.getTipoRelacionamento().name()))))
            .execute();
        
        // Se não encontrou na forma direta, tentar na forma invertida
        if (updated == 0) {
            TipoRelacionamento tipoInvertido = inverterTipoRelacionamento(existingRel.getTipoRelacionamento());
            dsl.update(PESSOA_RELACIONAMENTO)
                .set(PESSOA_RELACIONAMENTO.INICIO_RELACIONAMENTO, DateTimeHelper.toDb(newRel.getInicioRelacionamento()))
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(existingRel.getPessoaRelacionadaId())
                    .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaId))
                    .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(tipoInvertido.name()))))
                .execute();
        }
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
        
        List<Pessoa> people = dsl.selectFrom(PESSOA)
                .where(finalCondition)
                .fetch()
                .stream()
                .map(PessoaRepository::fromRepository)
                .toList();
        
        // Carregar relacionamentos para cada pessoa
        for (Pessoa pessoa : people) {
            List<Relacionamento> relationships = findRelationshipsByPersonId(pessoa.getId());
            pessoa.setRelacionamentos(relationships);
        }
        
        return people;
    }

    public List<PessoaHistory> findHistoryByPersonId(Long pessoaId) {
        // Buscar registros de histórico ordenados por data
        List<PessoaHistoryRecord> historyRecords = dsl.selectFrom(PESSOA_HISTORY)
            .where(PESSOA_HISTORY.PESSOA_ID.eq(pessoaId))
            .orderBy(PESSOA_HISTORY.ADDED_AT.desc())
            .fetch();

        List<PessoaHistory> history = new ArrayList<>();
        
        // Comparar registros adjacentes para identificar mudanças
        for (int i = 0; i < historyRecords.size(); i++) {
            PessoaHistoryRecord currentRecord = historyRecords.get(i);
            PessoaHistoryRecord previousRecord = (i + 1 < historyRecords.size()) ? historyRecords.get(i + 1) : null;
            
            List<PessoaHistoryChange> changes = compareHistoryEntries(currentRecord, previousRecord);
            
            if (!changes.isEmpty()) {
                // Buscar o nome do usuário que fez a modificação
                String updatedByUserName = null;
                if (currentRecord.getUpdatedBy() != null) {
                    UsuarioQuery query = UsuarioQuery.builder()
                        .id(currentRecord.getUpdatedBy())
                        .build();
                    List<Usuario> usuarios = usuarioRepository.find(query);
                    if (!usuarios.isEmpty()) {
                        updatedByUserName = usuarios.getFirst().getName();
                    }
                }
                
                PessoaHistory entry = new PessoaHistory(
                    DateTimeHelper.fromDb(currentRecord.getAddedAt()),
                    currentRecord.getUpdatedBy(),
                    updatedByUserName,
                    changes
                );
                history.add(entry);
            }
        }
        
        return history;
    }

    // CRUD para relacionamentos qualificados
    public Relacionamento insertRelationship(Long pessoaId, Relacionamento relacionamento) {
        PessoaRelacionamentoRecord relationamentoRecord = toRepository(relacionamento, pessoaId);
        PessoaRelacionamentoRecord saved = dsl.insertInto(PESSOA_RELACIONAMENTO)
                .set(relationamentoRecord)
                .returning()
                .fetchOne();
        return mapRelationship(saved, pessoaId);
    }
    
    /**
     * Busca todos os relacionamentos de uma pessoa, considerando que ela pode estar
     * tanto na coluna pessoa_id quanto na pessoa_relacionada_id
     */
    private List<Relacionamento> findRelationshipsByPersonId(Long pessoaId) {
        return dsl.selectFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId)
                    .or(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaId)))
                .fetch()
                .stream()
                .map(record -> mapRelationship(record, pessoaId))
                .toList();
    }
    
    /**
     * Mapeia um record de relacionamento para Relacionamento,
     * ajustando a perspectiva para a pessoa especificada
     */
    private Relacionamento mapRelationship(PessoaRelacionamentoRecord record, Long pessoaId) {
        Relacionamento rel = new Relacionamento();
        rel.setPessoaId(pessoaId);
        rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        
        boolean pessoalPrincipal = record.getPessoaId().equals(pessoaId);
        
        if (pessoalPrincipal) {
            // Pessoa atual é a principal - usar dados diretos
            rel.setPessoaRelacionadaId(record.getPessoaRelacionadaId());
            if (record.getTipoRelacionamento() != null) {
                rel.setTipoRelacionamento(
                    TipoRelacionamento.valueOf(record.getTipoRelacionamento().name())
                );
            }
        } else {
            // Pessoa atual é a relacionada - inverter perspectiva
            rel.setPessoaRelacionadaId(record.getPessoaId()); // Outra pessoa
            if (record.getTipoRelacionamento() != null) {
                TipoRelacionamento tipoOriginal = TipoRelacionamento.valueOf(record.getTipoRelacionamento().name());
                rel.setTipoRelacionamento(inverterTipoRelacionamento(tipoOriginal));
            }
        }
        
        // Buscar o nome da pessoa relacionada
        Long pessoaRelacionadaId = rel.getPessoaRelacionadaId();
        if (pessoaRelacionadaId != null) {
            PessoaRecord pessoaRelacionada = dsl.selectFrom(PESSOA)
                .where(PESSOA.PESSOA_ID.eq(pessoaRelacionadaId))
                .fetchOne();
            if (pessoaRelacionada != null) {
                rel.setNomePessoaRelacionada(pessoaRelacionada.getNome());
            }
        }
        
        return rel;
    }
    
    /**
     * Busca uma pessoa por ID (método auxiliar para evitar recursão infinita)
     */
    private Pessoa findPersonById(Long id) {
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
        if (query.getCategoria() != null) 
            conditions.add(PESSOA.CATEGORIA_ID.eq(query.getCategoria().getId()));
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
            p.setEstadoCivil(org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil.valueOf(pessoaRecord.getEstadoCivil().name()));
        p.setIgrejaAnterior(pessoaRecord.getIgrejaAnterior());
        p.setSituacaoIgrejaAnterior(pessoaRecord.getSituacaoIgrejaAnterior());
        p.setTempoNaIgreja(pessoaRecord.getTempoNaIgreja());
        p.setMotivosParaAdmissao(pessoaRecord.getMotivosParaAdmissao());
        if (pessoaRecord.getTipoBatismo() != null)
            p.setTipoBatismo(org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo.valueOf(pessoaRecord.getTipoBatismo().name()));
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
            p.setRegiao(Regiao.valueOf(pessoaRecord.getRegiao().name()));
        p.setFotoUrl(pessoaRecord.getFotoUrl());
        if (pessoaRecord.getSexo() != null)
            p.setSexo(Sexo.valueOf(pessoaRecord.getSexo().name()));
        p.setChefeDeFamiliaId(pessoaRecord.getChefeDeFamilia());
        if (pessoaRecord.getCategoriaId() != null)
            p.setCategoria(CategoriaEnum.fromId(pessoaRecord.getCategoriaId()));

        // Converter arrays do PostgreSQL para List<String>
        if (pessoaRecord.getEmailsSecundarios() != null) {
            p.setEmailsSecundarios(java.util.Arrays.asList(pessoaRecord.getEmailsSecundarios()));
        }
        if (pessoaRecord.getTelefonesSecundarios() != null) {
            p.setTelefonesSecundarios(java.util.Arrays.asList(pessoaRecord.getTelefonesSecundarios()));
        }
        p.setUpdatedByUserId(pessoaRecord.getUpdatedBy());

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
        pessoaRecord.setFotoUrl(pessoa.getFotoUrl());
        if (pessoa.getSexo() != null)
            pessoaRecord.setSexo(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(pessoa.getSexo().name()));
        pessoaRecord.setChefeDeFamilia(pessoa.getChefeDeFamiliaId());
        if (pessoa.getCategoria() != null) {
            pessoaRecord.setCategoriaId(pessoa.getCategoria().getId());
        }
        
        // Converter List<String> para arrays do PostgreSQL
        if (pessoa.getEmailsSecundarios() != null && !pessoa.getEmailsSecundarios().isEmpty()) {
            pessoaRecord.setEmailsSecundarios(pessoa.getEmailsSecundarios().toArray(new String[0]));
        }
        if (pessoa.getTelefonesSecundarios() != null && !pessoa.getTelefonesSecundarios().isEmpty()) {
            pessoaRecord.setTelefonesSecundarios(pessoa.getTelefonesSecundarios().toArray(new String[0]));
        }
        pessoaRecord.setUpdatedBy(pessoa.getUpdatedByUserId());
        
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
        pessoaHistoryRecord.setUpdatedBy(pessoaRecord.getUpdatedBy());

        return pessoaHistoryRecord;
    }

    /**
     * Compara dois registros de histórico e identifica as mudanças
     */
    private List<PessoaHistoryChange> compareHistoryEntries(PessoaHistoryRecord current, PessoaHistoryRecord previous) {
        List<PessoaHistoryChange> changes = new ArrayList<>();

        if (previous == null) {
            // Primeiro registro - não mostrar como mudança
            return changes;
        }

        // Comparar cada campo
        compareAttribute(changes, "nome", previous.getNome(), current.getNome());
        compareAttribute(changes, "apelido", previous.getApelido(), current.getApelido());
        compareAttribute(changes, "email", previous.getEmail(), current.getEmail());
        compareAttribute(changes, "telefone", previous.getTelefone(), current.getTelefone());
        compareAttribute(changes, "campus", previous.getCampus(), current.getCampus());
        compareAttribute(changes, "estadoCivil", previous.getEstadoCivil(), current.getEstadoCivil());
        compareAttribute(changes, "tipoBatismo", previous.getTipoBatismo(), current.getTipoBatismo());
        compareAttribute(changes, "dataBatismo", previous.getDataBatismo(), current.getDataBatismo());
        compareAttribute(changes, "dataProfissaoDeFe", previous.getDataProfissaoDeFe(), current.getDataProfissaoDeFe());
        compareAttribute(changes, "igrejaBatismo", previous.getIgrejaBatismo(), current.getIgrejaBatismo());
        compareAttribute(changes, "profissao", previous.getProfissao(), current.getProfissao());
        compareAttribute(changes, "empresa", previous.getEmpresa(), current.getEmpresa());
        compareAttribute(changes, "enderecoCep", previous.getEnderecoCep(), current.getEnderecoCep());
        compareAttribute(changes, "enderecoLogradouro", previous.getEnderecoLogradouro(), current.getEnderecoLogradouro());
        compareAttribute(changes, "enderecoNumero", previous.getEnderecoNumero(), current.getEnderecoNumero());
        compareAttribute(changes, "enderecoComplemento", previous.getEnderecoComplemento(), current.getEnderecoComplemento());
        compareAttribute(changes, "regiao", previous.getRegiao(), current.getRegiao());
        compareAttribute(changes, "fotoUrl", previous.getFotoUrl(), current.getFotoUrl());
        compareAttribute(changes, "chefeDeFamilia", previous.getChefeDeFamilia(), current.getChefeDeFamilia());
        compareAttribute(changes, "categoriaId", previous.getCategoriaId(), current.getCategoriaId());

        // Comparar arrays
        compareArrays(changes, "emailsSecundarios", previous.getEmailsSecundarios(), current.getEmailsSecundarios());
        compareArrays(changes, "telefonesSecundarios", previous.getTelefonesSecundarios(), current.getTelefonesSecundarios());

        return changes;
    }

    /**
     * Compara um campo individual e adiciona à lista de mudanças se houver diferença
     */
    private void compareAttribute(List<PessoaHistoryChange> changes, String attribute, Object oldValue, Object newValue) {
        // Normalizar valores nulos
        String old = (oldValue != null) ? oldValue.toString() : null;
        String current = (newValue != null) ? newValue.toString() : null;
        
        // Verificar se houve mudança
        if (!java.util.Objects.equals(old, current)) {
            PessoaHistoryChange change = new PessoaHistoryChange(attribute, old, current);
            changes.add(change);
        }
    }

    /**
     * Compara arrays e adiciona à lista de mudanças se houver diferença
     */
    private void compareArrays(List<PessoaHistoryChange> changes, String attribute, String[] oldArray, String[] newArray) {
        // Converter arrays para listas para facilitar comparação
        List<String> oldList = (oldArray != null) ? Arrays.asList(oldArray) : new ArrayList<>();
        List<String> newList = (newArray != null) ? Arrays.asList(newArray) : new ArrayList<>();
        
        // Verificar se houve mudança
        if (!oldList.equals(newList)) {
            String old = oldList.isEmpty() ? null : String.join(", ", oldList);
            String current = newList.isEmpty() ? null : String.join(", ", newList);
            
            PessoaHistoryChange change = new PessoaHistoryChange(attribute, old, current);
            changes.add(change);
        }
    }


    private static PessoaRelacionamentoRecord toRepository(Relacionamento relacionamento, Long pessoaId) {
        PessoaRelacionamentoRecord record = new PessoaRelacionamentoRecord();

        record.setPessoaId(pessoaId);
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