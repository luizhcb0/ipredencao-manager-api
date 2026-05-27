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

/**
 * Gera o texto de uma linha do relatório de ata para um {@link OfficialAct}.
 * <p>
 * Cada forma do Cap. III da CI/IPB tem um template específico que combina dados da
 * pessoa, do ato e da metadata. Campos ausentes recebem placeholders entre colchetes
 * (ex.: {@code [endereço não cadastrado]}) para que o redator perceba e complete.
 */
@Component
public class MinuteReportFormatter {

    private static final DateTimeFormatter BR_DATE = DateTimeFormat.forPattern("dd/MM/yyyy");

    public String format(OfficialAct act, Pessoa pessoa) {
        OfficialActFormEnum form = OfficialActFormEnum.fromId(act.getOfficialActFormId());
        Map<String, Object> md = act.getMetadata() != null ? act.getMetadata() : Map.of();
        return switch (form) {
            // ===== Admissão de membro comungante (Art. 16) =====
            case ADM_MC_PROFISSAO_FE ->
                buildAdmissaoMc(act, pessoa, null, "por profissão de fé, " + celebrante(md));
            case ADM_MC_PROFISSAO_FE_E_BATISMO ->
                buildAdmissaoMc(act, pessoa, null, "por profissão de fé e batismo, " + celebrante(md));
            case ADM_MC_CARTA_TRANSFERENCIA ->
                buildAdmissaoMc(act, pessoa, proveniencia(md), "por carta de transferência");
            case ADM_MC_JURISDICAO_A_PEDIDO ->
                buildAdmissaoMc(act, pessoa, proveniencia(md), "por jurisdição a pedido");
            case ADM_MC_JURISDICAO_EX_OFFICIO ->
                buildAdmissaoMc(act, pessoa, proveniencia(md), "por jurisdição ex officio");
            case ADM_MC_RESTAURACAO ->
                buildAdmissaoMc(act, pessoa, null, "por restauração" + motivoOpcional(md));
            case ADM_MC_DESIGNACAO_PRESBITERIO ->
                buildAdmissaoMc(act, pessoa, null, "por designação do Presbitério "
                        + str(md, "presbiterioOrigem", "[presbitério não informado]")
                        + numeroResolucaoOpcional(md));

            // ===== Admissão de membro não comungante (Art. 17) =====
            case ADM_MNC_BATISMO_INFANCIA ->
                buildAdmissaoMnc(act, pessoa, "por batismo, " + celebrante(md));
            case ADM_MNC_TRANSFERENCIA_PAIS ->
                buildAdmissaoMnc(act, pessoa, "por transferência dos pais ou responsáveis, vindos da "
                        + igrejaOrigem(md) + presbiterioOrigem(md));
            case ADM_MNC_JURISDICAO_PAIS ->
                buildAdmissaoMnc(act, pessoa,
                        "por jurisdição assumida sobre os pais ou responsáveis, vindos da "
                        + igrejaOrigem(md) + presbiterioOrigem(md));

            // ===== Demissão de membro comungante (Art. 23) =====
            case DEM_MC_EXCLUSAO_DISCIPLINA ->
                buildDemissaoSimples(act, pessoa, "por exclusão por disciplina"
                        + motivoOpcional(md)
                        + opcional(md, "numeroProcessoDisciplinar", ", processo nº "));
            case DEM_MC_EXCLUSAO_A_PEDIDO ->
                buildDemissaoSimples(act, pessoa, "por exclusão a pedido" + motivoOpcional(md));
            case DEM_MC_EXCLUSAO_AUSENCIA ->
                buildDemissaoSimples(act, pessoa, "por exclusão por ausência" + motivoOpcional(md));
            case DEM_MC_CARTA_TRANSFERENCIA ->
                buildDemissaoSimples(act, pessoa, "por carta de transferência, destinada à "
                        + igrejaDestino(md) + presbiterioDestino(md));
            case DEM_MC_JURISDICAO_OUTRA_IGREJA ->
                buildDemissaoSimples(act, pessoa, "por jurisdição assumida por outra igreja, a "
                        + igrejaDestino(md) + presbiterioDestino(md));
            case DEM_MC_ORDENACAO_MINISTRO ->
                buildDemissaoSimples(act, pessoa, "por ordenação ao ministério, transferido ao Presbitério "
                        + str(md, "presbiterioOrigem", "[presbitério não informado]")
                        + numeroResolucaoOpcional(md));
            case DEM_MC_FALECIMENTO ->
                buildFalecimento(act, pessoa, md);

            // ===== Demissão de membro não comungante (Art. 24) =====
            case DEM_MNC_TRANSF_PAIS ->
                buildDemissaoSimples(act, pessoa, "por carta de transferência dos pais ou responsáveis, destinada à "
                        + igrejaDestino(md) + presbiterioDestino(md));
            case DEM_MNC_TRANSF_PROPRIA ->
                buildDemissaoSimples(act, pessoa, "por carta de transferência, destinada à "
                        + igrejaDestino(md) + presbiterioDestino(md));
            case DEM_MNC_MAIORIDADE ->
                buildDemissaoSimples(act, pessoa, "por atingimento da maioridade (18 anos)"
                        + motivoOpcional(md));
            case DEM_MNC_PROFISSAO_FE ->                              // Art. 24, d — promoção
                buildPromocaoArt24d(act, pessoa, md);
            case DEM_MNC_SOLIC_PAIS_OUTRA ->
                buildDemissaoSimples(act, pessoa,
                        "por solicitação dos pais (aderiram a outra comunidade), destinada à "
                        + igrejaDestino(md) + presbiterioDestino(md) + motivoOpcional(md));
            case DEM_MNC_FALECIMENTO ->
                buildFalecimento(act, pessoa, md);
        };
    }

    // ===== Builders por categoria =====

    /**
     * Linha de admissão de membro comungante. Inclui data de nascimento, estado civil
     * e endereço (quando cadastrados). Pais e a palavra "comungante" não são emitidos
     * conforme requisito de produto (e o regulamento Art. 12, §2º, II não os exige).
     *
     * @param proveniencia trecho opcional "proveniente da X, Presbitério de Y" para formas
     *                     com {@code igrejaOrigem}; {@code null} para as demais.
     * @param causa trecho final "por <modo de recepção>".
     */
    private String buildAdmissaoMc(OfficialAct act, Pessoa pessoa, String proveniencia, String causa) {
        StringBuilder sb = new StringBuilder();
        sb.append("em ").append(formatDate(act.getActDate())).append(", ")
                .append(personHeader(act, pessoa));
        appendIfNotBlank(sb, ", ", birth(pessoa));
        appendIfNotBlank(sb, ", ", estadoCivil(pessoa));
        appendIfNotBlank(sb, ", ", residencia(pessoa));
        appendIfNotBlank(sb, ", ", proveniencia);
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
        String genero = isFeminino(pessoa) ? "Faleceu" : "Faleceu";
        StringBuilder sb = new StringBuilder();
        sb.append(genero).append(" em ").append(formatDate(act.getActDate())).append(", ")
                .append(personHeader(act, pessoa));
        appendIfNotBlank(sb, ", causa mortis: ", str(md, "causaMortis", null));
        DateTime cer = dt(md, "dataCerimoniaFunebre");
        if (cer != null) {
            sb.append(", cerimônia fúnebre em ").append(formatDate(cer));
        }
        appendIfNotBlank(sb, ", em ", str(md, "localCerimoniaFunebre", null));
        appendIfNotBlank(sb, ", oficiada por ", str(md, "celebranteFunebreNome", null));
        sb.append(".");
        return sb.toString();
    }

    private String buildPromocaoArt24d(OfficialAct act, Pessoa pessoa, Map<String, Object> md) {
        return "em " + formatDate(act.getActDate()) + ", " + personHeader(act, pessoa)
                + ", admitido(a) à comunhão plena por profissão de fé (Art. 24, d), "
                + celebrante(md) + ".";
    }

    // ===== Helpers de campos =====

    /**
     * Nome em maiúsculas seguido do número de ordem de admissão entre parênteses
     * ("à margem interna" no regulamento Art. 12, §2º, III). Quando o ato não carrega
     * o número (demissões, falecimentos, etc.), os parênteses são omitidos.
     */
    private String personHeader(OfficialAct act, Pessoa pessoa) {
        String nome = pessoa.getNome() != null ? pessoa.getNome().toUpperCase() : "[NOME NÃO CADASTRADO]";
        Long numero = act.getNumeroOrdemAdmissao();
        return numero != null ? nome + " (" + numero + ")" : nome;
    }

    /**
     * Retorna "filho(a) de Pai e Mãe" quando ambos cadastrados, "filho(a) de Pai"
     * ou "filha de Mãe" quando só um existir, ou {@code null} quando nenhum estiver
     * cadastrado (chamador omite o bloco completamente).
     */
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

    // ===== Helpers de metadata =====

    /**
     * Lê o nome do celebrante a partir de {@code metadata.celebrante}, que é armazenado
     * como objeto {@code {id?, name}} (PERSON_REF). Quando o campo está ausente ou sem
     * {@code name}, retorna placeholder para o redator perceber e completar.
     */
    private String celebrante(Map<String, Object> md) {
        Object v = md.get("celebrante");
        String nome = null;
        if (v instanceof Map<?, ?> m) {
            Object n = m.get("name");
            nome = n != null ? n.toString() : null;
        } else if (v instanceof String s) {
            nome = s;
        }
        return isBlank(nome) ? "por [celebrante]" : "por " + nome;
    }

    private String igrejaOrigem(Map<String, Object> md) {
        return str(md, "igrejaOrigem", "[igreja de origem não informada]");
    }

    /**
     * Trecho "proveniente da {@code igrejaOrigem}[, Presbitério de X]" para inserção
     * antes da causa em admissões MC. Mantém o placeholder de igreja quando ausente,
     * já que essas formas exigem {@code igrejaOrigem} no schema.
     */
    private String proveniencia(Map<String, Object> md) {
        return "proveniente da " + igrejaOrigem(md) + presbiterioOrigem(md);
    }

    private String igrejaDestino(Map<String, Object> md) {
        return str(md, "igrejaDestino", "[igreja de destino não informada]");
    }

    private String presbiterioOrigem(Map<String, Object> md) {
        String v = str(md, "presbiterioOrigem", null);
        return isBlank(v) ? "" : ", Presbitério de " + v;
    }

    private String presbiterioDestino(Map<String, Object> md) {
        String v = str(md, "presbiterioDestino", null);
        return isBlank(v) ? "" : ", Presbitério de " + v;
    }

    private String motivoOpcional(Map<String, Object> md) {
        String v = str(md, "motivo", null);
        return isBlank(v) ? "" : ", motivo: " + v;
    }

    private String numeroResolucaoOpcional(Map<String, Object> md) {
        String v = str(md, "numeroResolucao", null);
        return isBlank(v) ? "" : ", resolução nº " + v;
    }

    private String opcional(Map<String, Object> md, String key, String prefix) {
        String v = str(md, key, null);
        return isBlank(v) ? "" : prefix + v;
    }

    // ===== Conversão segura =====

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
