package org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history;

import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaHistoryRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PessoaHistoryDiff {

    public static List<PessoaHistoryChange> compare(PessoaHistoryRecord current, PessoaHistoryRecord previous) {
        List<PessoaHistoryChange> changes = new ArrayList<>();
        if (previous == null) return changes;

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
        compareAttribute(changes, "enderecoId", previous.getEnderecoId(), current.getEnderecoId());
        compareAttribute(changes, "fotoUrl", previous.getFotoUrl(), current.getFotoUrl());
        compareAttribute(changes, "chefeDeFamilia", previous.getChefeDeFamilia(), current.getChefeDeFamilia());
        compareAttribute(changes, "categoriaId", previous.getCategoriaId(), current.getCategoriaId());

        compareArrays(changes, "profissao", previous.getProfissao(), current.getProfissao());
        compareArrays(changes, "empresa", previous.getEmpresa(), current.getEmpresa());
        compareArrays(changes, "emailsSecundarios", previous.getEmailsSecundarios(), current.getEmailsSecundarios());
        compareArrays(changes, "telefonesSecundarios", previous.getTelefonesSecundarios(), current.getTelefonesSecundarios());

        return changes;
    }

    private static void compareAttribute(List<PessoaHistoryChange> changes, String attribute, Object oldValue, Object newValue) {
        String old = (oldValue != null) ? oldValue.toString() : null;
        String current = (newValue != null) ? newValue.toString() : null;
        if (!Objects.equals(old, current)) {
            changes.add(new PessoaHistoryChange(attribute, old, current));
        }
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
