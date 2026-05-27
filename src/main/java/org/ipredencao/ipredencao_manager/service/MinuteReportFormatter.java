package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Gera o texto de cada linha do relatório de ata. Campos ausentes recebem placeholders entre colchetes. */
@Component
public class MinuteReportFormatter {

    private static final DateTimeFormatter BR_DATE = DateTimeFormat.forPattern("dd/MM/yyyy");

    public String format(OfficialAct act, Pessoa pessoa) {
        OfficialActFormEnum form = OfficialActFormEnum.fromId(act.getOfficialActFormId());
        Map<String, Object> md = act.getMetadata() != null ? act.getMetadata() : Map.of();
        return switch (form) {
            // ===== Admissão de membro comungante (Art. 16) =====
            case ADM_MC_PROFISSAO_FE ->
                buildAdmissaoMc(act, pessoa, null, "por profissão de fé, " + celebrant(md));
            case ADM_MC_PROFISSAO_FE_E_BATISMO ->
                buildAdmissaoMc(act, pessoa, null, "por profissão de fé e batismo, " + celebrant(md));
            case ADM_MC_CARTA_TRANSFERENCIA ->
                buildAdmissaoMc(act, pessoa, provenance(md), "por carta de transferência");
            case ADM_MC_JURISDICAO_A_PEDIDO ->
                buildAdmissaoMc(act, pessoa, provenance(md), "por jurisdição a pedido");
            case ADM_MC_JURISDICAO_EX_OFFICIO ->
                buildAdmissaoMc(act, pessoa, provenance(md), "por jurisdição ex officio");
            case ADM_MC_RESTAURACAO ->
                buildAdmissaoMc(act, pessoa, null, "por restauração" + optionalReason(md));
            case ADM_MC_DESIGNACAO_PRESBITERIO ->
                buildAdmissaoMc(act, pessoa, null, "por designação do Presbitério "
                        + str(md, "originPresbytery", "[presbitério não informado]")
                        + optionalResolutionNumber(md));

            // ===== Admissão de membro não comungante (Art. 17) =====
            case ADM_MNC_BATISMO_INFANCIA ->
                buildAdmissaoMnc(act, pessoa, "por batismo, " + celebrant(md));
            case ADM_MNC_TRANSFERENCIA_PAIS ->
                buildAdmissaoMnc(act, pessoa, "por transferência dos pais ou responsáveis, vindos da "
                        + originChurch(md) + originPresbytery(md));
            case ADM_MNC_JURISDICAO_PAIS ->
                buildAdmissaoMnc(act, pessoa,
                        "por jurisdição assumida sobre os pais ou responsáveis, vindos da "
                        + originChurch(md) + originPresbytery(md));

            // ===== Demissão de membro comungante (Art. 23) =====
            case DEM_MC_EXCLUSAO_DISCIPLINA ->
                buildDemissaoSimples(act, pessoa, "por exclusão por disciplina"
                        + optionalReason(md)
                        + optional(md, "disciplinaryProcessNumber", ", processo nº "));
            case DEM_MC_EXCLUSAO_A_PEDIDO ->
                buildDemissaoSimples(act, pessoa, "por exclusão a pedido" + optionalReason(md));
            case DEM_MC_EXCLUSAO_AUSENCIA ->
                buildDemissaoSimples(act, pessoa, "por exclusão por ausência" + optionalReason(md));
            case DEM_MC_CARTA_TRANSFERENCIA ->
                buildDemissaoSimples(act, pessoa, "por carta de transferência, destinada à "
                        + destinationChurch(md) + destinationPresbytery(md));
            case DEM_MC_JURISDICAO_OUTRA_IGREJA ->
                buildDemissaoSimples(act, pessoa, "por jurisdição assumida por outra igreja, a "
                        + destinationChurch(md) + destinationPresbytery(md));
            case DEM_MC_ORDENACAO_MINISTRO ->
                buildDemissaoSimples(act, pessoa, "por ordenação ao ministério, transferido ao Presbitério "
                        + str(md, "originPresbytery", "[presbitério não informado]")
                        + optionalResolutionNumber(md));
            case DEM_MC_FALECIMENTO ->
                buildFalecimento(act, pessoa, md);

            // ===== Demissão de membro não comungante (Art. 24) =====
            case DEM_MNC_TRANSF_PAIS ->
                buildDemissaoSimples(act, pessoa, "por carta de transferência dos pais ou responsáveis, destinada à "
                        + destinationChurch(md) + destinationPresbytery(md));
            case DEM_MNC_TRANSF_PROPRIA ->
                buildDemissaoSimples(act, pessoa, "por carta de transferência, destinada à "
                        + destinationChurch(md) + destinationPresbytery(md));
            case DEM_MNC_MAIORIDADE ->
                buildDemissaoSimples(act, pessoa, "por atingimento da maioridade (18 anos)"
                        + optionalReason(md));
            case DEM_MNC_PROFISSAO_FE ->                              // Art. 24, d — promoção
                buildPromocaoArt24d(act, pessoa, md);
            case DEM_MNC_SOLIC_PAIS_OUTRA ->
                buildDemissaoSimples(act, pessoa,
                        "por solicitação dos pais (aderiram a outra comunidade), destinada à "
                        + destinationChurch(md) + destinationPresbytery(md) + optionalReason(md));
            case DEM_MNC_FALECIMENTO ->
                buildFalecimento(act, pessoa, md);
        };
    }

    private String buildAdmissaoMc(OfficialAct act, Pessoa pessoa, String provenance, String causa) {
        StringBuilder sb = new StringBuilder();
        sb.append("em ").append(formatDate(act.getActDate())).append(", ")
                .append(personHeader(act, pessoa));
        appendIfNotBlank(sb, ", ", birth(pessoa));
        appendIfNotBlank(sb, ", ", estadoCivil(pessoa));
        appendIfNotBlank(sb, ", ", residencia(pessoa));
        appendIfNotBlank(sb, ", ", provenance);
        sb.append(", ").append(causa).append(".");
        return sb.toString();
    }

    private String buildAdmissaoMnc(OfficialAct act, Pessoa pessoa, String causa) {
        StringBuilder sb = new StringBuilder();
        sb.append("em ").append(formatDate(act.getActDate())).append(", ")
                .append(personHeader(act, pessoa));
        appendIfNotBlank(sb, ", ", parents(pessoa));
        appendIfNotBlank(sb, ", ", birth(pessoa));
        appendIfNotBlank(sb, ", ", estadoCivil(pessoa));
        appendIfNotBlank(sb, ", ", residencia(pessoa));
        sb.append(", ").append(causa).append(".");
        return sb.toString();
    }

    private String buildDemissaoSimples(OfficialAct act, Pessoa pessoa, String causa) {
        return "em " + formatDate(act.getActDate()) + ", " + personHeader(act, pessoa)
                + ", " + causa + ".";
    }

    private String buildFalecimento(OfficialAct act, Pessoa pessoa, Map<String, Object> md) {
        StringBuilder sb = new StringBuilder();
        sb.append("Faleceu em ").append(formatDate(act.getActDate())).append(", ")
                .append(personHeader(act, pessoa));
        appendIfNotBlank(sb, ", causa mortis: ", str(md, "causeOfDeath", null));
        DateTime cer = dt(md, "funeralCeremonyDate");
        if (cer != null) {
            sb.append(", cerimônia fúnebre em ").append(formatDate(cer));
        }
        appendIfNotBlank(sb, ", em ", str(md, "funeralCeremonyLocation", null));
        appendIfNotBlank(sb, ", oficiada por ", str(md, "funeralCelebrantName", null));
        sb.append(".");
        return sb.toString();
    }

    private String buildPromocaoArt24d(OfficialAct act, Pessoa pessoa, Map<String, Object> md) {
        return "em " + formatDate(act.getActDate()) + ", " + personHeader(act, pessoa)
                + ", admitido(a) à comunhão plena por profissão de fé (Art. 24, d), "
                + celebrant(md) + ".";
    }

    private String personHeader(OfficialAct act, Pessoa pessoa) {
        String nome = pessoa.getNome() != null ? pessoa.getNome().toUpperCase() : "[NOME NÃO CADASTRADO]";
        Long numero = act.getAdmissionOrderNumber();
        return numero != null ? nome + " (" + numero + ")" : nome;
    }

    private String parents(Pessoa pessoa) {
        if (pessoa.getRelacionamentos() == null || pessoa.getRelacionamentos().isEmpty()) {
            return null;
        }
        String pai = findParentName(pessoa, TipoRelacionamento.PAI);
        String mae = findParentName(pessoa, TipoRelacionamento.MAE);
        if (pai == null && mae == null) return null;
        if (pai != null && mae != null) return filhoPrefix(pessoa) + " " + pai + " e " + mae;
        return filhoPrefix(pessoa) + " " + (pai != null ? pai : mae);
    }

    private String findParentName(Pessoa pessoa, TipoRelacionamento tipo) {
        for (Relacionamento r : pessoa.getRelacionamentos()) {
            if (tipo.equals(r.getTipoRelacionamento())) {
                return r.getNomePessoaRelacionada();
            }
        }
        return null;
    }

    private String filhoPrefix(Pessoa pessoa) {
        return isFeminino(pessoa) ? "filha de" : "filho de";
    }

    private String birth(Pessoa pessoa) {
        if (pessoa.getDataNascimento() == null) return null;
        return (isFeminino(pessoa) ? "nascida em " : "nascido em ") + formatDate(pessoa.getDataNascimento());
    }

    private String estadoCivil(Pessoa pessoa) {
        EstadoCivil ec = pessoa.getEstadoCivil();
        if (ec == null) return null;
        boolean fem = isFeminino(pessoa);
        return switch (ec) {
            case CASADO -> fem ? "casada" : "casado";
            case SOLTEIRO_SEM_RELACIONAMENTO, SOLTEIRO_NAMORANDO, SOLTEIRO_NOIVO ->
                fem ? "solteira" : "solteiro";
            case DIVORCIADO_SEM_RELACIONAMENTO, DIVORCIADO_NAMORANDO, DIVORCIADO_NOIVO ->
                fem ? "divorciada" : "divorciado";
            case VIUVO_SEM_RELACIONAMENTO, VIUVO_NAMORANDO, VIUVO_NOIVO ->
                fem ? "viúva" : "viúvo";
        };
    }

    private String residencia(Pessoa pessoa) {
        Endereco e = pessoa.getEndereco();
        if (e == null || isBlank(e.getLogradouro())) {
            return "com [endereço não cadastrado]";
        }
        StringBuilder sb = new StringBuilder("com residência e domicílio em ");
        sb.append(e.getLogradouro());
        appendIfNotBlank(sb, ", ", e.getNumero());
        appendIfNotBlank(sb, ", ", e.getComplemento());
        appendIfNotBlank(sb, " - ", e.getBairro());
        if (!isBlank(e.getCidade()) || !isBlank(e.getEstado())) {
            sb.append(", ");
            if (!isBlank(e.getCidade())) sb.append(e.getCidade());
            if (!isBlank(e.getCidade()) && !isBlank(e.getEstado())) sb.append("/");
            if (!isBlank(e.getEstado())) sb.append(e.getEstado());
        }
        appendIfNotBlank(sb, ", CEP ", e.getCep());
        return sb.toString();
    }

    private String celebrant(Map<String, Object> md) {
        Object v = md.get("celebrant");
        String nome = null;
        if (v instanceof Map<?, ?> m) {
            Object n = m.get("name");
            nome = n != null ? n.toString() : null;
        } else if (v instanceof String s) {
            nome = s;
        }
        return isBlank(nome) ? "por [celebrante]" : "por " + nome;
    }

    private String originChurch(Map<String, Object> md) {
        return str(md, "originChurch", "[igreja de origem não informada]");
    }

    private String provenance(Map<String, Object> md) {
        return "proveniente da " + originChurch(md) + originPresbytery(md);
    }

    private String destinationChurch(Map<String, Object> md) {
        return str(md, "destinationChurch", "[igreja de destino não informada]");
    }

    private String originPresbytery(Map<String, Object> md) {
        String v = str(md, "originPresbytery", null);
        return isBlank(v) ? "" : ", Presbitério de " + v;
    }

    private String destinationPresbytery(Map<String, Object> md) {
        String v = str(md, "destinationPresbytery", null);
        return isBlank(v) ? "" : ", Presbitério de " + v;
    }

    private String optionalReason(Map<String, Object> md) {
        String v = str(md, "reason", null);
        return isBlank(v) ? "" : ", motivo: " + v;
    }

    private String optionalResolutionNumber(Map<String, Object> md) {
        String v = str(md, "resolutionNumber", null);
        return isBlank(v) ? "" : ", resolução nº " + v;
    }

    private String optional(Map<String, Object> md, String key, String prefix) {
        String v = str(md, key, null);
        return isBlank(v) ? "" : prefix + v;
    }

    private static String str(Map<String, Object> md, String key, String fallback) {
        Object v = md.get(key);
        if (v == null) return fallback;
        String s = v.toString();
        return s.isBlank() ? fallback : s;
    }

    private static DateTime dt(Map<String, Object> md, String key) {
        Object v = md.get(key);
        if (v == null) return null;
        if (v instanceof DateTime dt) return dt;
        if (v instanceof String s && !s.isBlank()) {
            try {
                return DateTime.parse(s);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String formatDate(DateTime dt) {
        return dt != null ? BR_DATE.print(dt) : "[data não informada]";
    }

    private static boolean isFeminino(Pessoa pessoa) {
        return pessoa.getSexo() == Sexo.FEMININO;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void appendIfNotBlank(StringBuilder sb, String prefix, String value) {
        if (!isBlank(value)) sb.append(prefix).append(value);
    }
}
