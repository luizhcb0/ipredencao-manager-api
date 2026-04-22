package org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history;

import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaHistoryRecord;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class PessoaHistoryDiff {

    public static List<PessoaHistoryChange> compare(
            PessoaHistoryRecord current,
            PessoaHistoryRecord previous,
            Map<Long, String> chefeDeFamiliaNomes
    ) {
        List<PessoaHistoryChange> changes = new ArrayList<>();
        if (previous == null) return changes;

        compareAttribute(changes, "Nome", previous.getNome(), current.getNome());
        compareAttribute(changes, "Apelido", previous.getApelido(), current.getApelido());
        compareAttribute(changes, "Email", previous.getEmail(), current.getEmail());
        compareAttribute(changes, "Telefone", previous.getTelefone(), current.getTelefone());
        compareAttribute(changes, "Campus", previous.getCampus(), current.getCampus());
        compareAttribute(changes, "Estado Civil", previous.getEstadoCivil(), current.getEstadoCivil());
        compareAttribute(changes, "Tipo de Batismo", previous.getTipoBatismo(), current.getTipoBatismo());
        compareAttribute(changes, "Data de Batismo", previous.getDataBatismo(), current.getDataBatismo());
        compareAttribute(changes, "Data de Profissão de Fé", previous.getDataProfissaoDeFe(), current.getDataProfissaoDeFe());
        compareAttribute(changes, "Igreja de Batismo", previous.getIgrejaBatismo(), current.getIgrejaBatismo());

        compareResolved(changes, "Categoria",
            previous.getCategoriaId(), current.getCategoriaId(),
            id -> CategoriaEnum.fromId(id).getNome());

        compareResolved(changes, "Chefe de Família",
            previous.getChefeDeFamilia(), current.getChefeDeFamilia(),
            chefeDeFamiliaNomes::get);

        compareArrays(changes, "Profissão", previous.getProfissao(), current.getProfissao());
        compareArrays(changes, "Empresa", previous.getEmpresa(), current.getEmpresa());
        compareArrays(changes, "Emails Secundários", previous.getEmailsSecundarios(), current.getEmailsSecundarios());
        compareArrays(changes, "Telefones Secundários", previous.getTelefonesSecundarios(), current.getTelefonesSecundarios());

        return changes;
    }

    private static void compareAttribute(List<PessoaHistoryChange> changes, String attribute, Object oldValue, Object newValue) {
        String old = (oldValue != null) ? oldValue.toString() : null;
        String current = (newValue != null) ? newValue.toString() : null;
        if (!Objects.equals(old, current)) {
            changes.add(new PessoaHistoryChange(attribute, old, current));
        }
    }

    private static void compareResolved(
            List<PessoaHistoryChange> changes,
            String attribute,
            Long oldId,
            Long newId,
            Function<Long, String> resolver
    ) {
        if (Objects.equals(oldId, newId)) return;
        String old = (oldId != null) ? resolver.apply(oldId) : null;
        String current = (newId != null) ? resolver.apply(newId) : null;
        changes.add(new PessoaHistoryChange(attribute, old, current));
    }

    private static void compareArrays(List<PessoaHistoryChange> changes, String attribute, String[] oldArray, String[] newArray) {
        List<String> oldList = (oldArray != null) ? Arrays.asList(oldArray) : List.of();
        List<String> newList = (newArray != null) ? Arrays.asList(newArray) : List.of();
        if (!oldList.equals(newList)) {
            String old = oldList.isEmpty() ? null : String.join(", ", oldList);
            String current = newList.isEmpty() ? null : String.join(", ", newList);
            changes.add(new PessoaHistoryChange(attribute, old, current));
        }
    }
}
