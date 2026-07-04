package org.ipredencao.ipredencao_manager.model.pregnancy;

import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.joda.time.DateTime;

/** Montagem de nome e apelido para registros de gestação (padrão legado). */
public final class PregnancyNaming {

    private PregnancyNaming() {}

    public static String displayName(Pessoa person) {
        if (person == null) return "";
        if (person.getApelido() != null && !person.getApelido().isBlank()) {
            return person.getApelido().trim();
        }
        return person.getNome() != null ? person.getNome().trim() : "";
    }

    public static String buildNickname(Pessoa mother, Pessoa father) {
        String motherName = displayName(mother);
        if (father != null) {
            return "(bebê de " + motherName + " e " + displayName(father) + ")";
        }
        return "(bebê de " + motherName + ")";
    }

    public static String buildName(String name, String nickname) {
        StringBuilder sb = new StringBuilder();
        if (name != null && !name.isBlank()) {
            sb.append(name.trim()).append(" ");
        }
        sb.append(nickname);
        return sb.toString().trim();
    }

    /** Primeiro token do nome completo (apelido pós-nascimento). */
    public static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}
