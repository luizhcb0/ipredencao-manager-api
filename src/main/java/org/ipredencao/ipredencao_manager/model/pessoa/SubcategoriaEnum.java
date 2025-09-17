package org.ipredencao.ipredencao_manager.model.pessoa;

public enum SubcategoriaEnum {
    CATEGORIA_DESCONHECIDA("-1", "Categoria Desconhecida", -1L),
    // Categoria 00 (Pastores)
    PASTORES_DA_IGREJA("00", "Pastores da Igreja", 1L),
    
    // Categoria 01 (Membros comungantes)
    MEMBRO("01", "Membro", 2L),
    
    // Categoria 02 (Membros comungantes - rol à parte)
    ROL_A_PARTE_MEMBRO_EM_TRANSITO("02.01", "Rol à parte, membro em trânsito", 3L),
    ROL_A_PARTE_MEMBRO_AUSENTE("02.03", "Rol à parte, membro ausente (doença etc.)", 4L),
    ROL_A_PARTE_MEMBRO_A_TRANSFERIR("02.90", "Rol à parte, membro a transferir", 5L),
    ROL_A_PARTE_MEMBRO_NAO_LOCALIZADO("02.95", "Rol à parte, membro não localizado ou pedido de desligamento", 6L),
    ROL_A_PARTE_MEMBRO_SOB_DISCIPLINA("02.99", "Rol à parte, membro sob disciplina", 7L),
    
    // Categoria 03 (Membros não comungantes)
    MEMBRO_NAO_COMUNGANTE_AGUARDANDO_PROFISSAO("03.00", "Membro não comungante, aguardando profissão de fé (admitindo)", 8L),
    MEMBRO_NAO_COMUNGANTE_AGUARDANDO_EXAME("03.01", "Membro não comungante, aguardando exame para profissão de fé (admitindo)", 9L),
    MEMBRO_NAO_COMUNGANTE_EM_CATEQUIZACAO("03.02", "Membro não comungante, em catequização final", 10L),
    MEMBRO_NAO_COMUNGANTE_ESPECIAL("03.95", "Membro não comungante (especial), não requer profissão de fé", 11L),
    MEMBRO_NAO_COMUNGANTE("03.96", "Membro não comungante", 12L),
    MEMBRO_NAO_COMUNGANTE_IDADE_PROFISSAO("03.97", "Membro não comungante, em idade para profissão de fé", 13L),
    MEMBRO_NAO_COMUNGANTE_ROL_A_PARTE_TRANSITO("03.98", "Membro não comungante, rol à parte (em trânsito)", 14L),
    MEMBRO_NAO_COMUNGANTE_ROL_A_PARTE_TRANSF("03.99", "Membro não comungante, rol à parte (transf)", 15L),
    
    // Categoria 04 (Admissão)
    ADMISSAO_ROTINA("04.00", "Admissão, rotina de admissão", 16L),
    ADMISSAO_AGUARDANDO_REGISTRO("04.01", "Admissão, aguardando registro em ata", 17L),
    ADMISSAO_AGUARDANDO_CARTA("04.98", "Admissão, aguardando carta de transferência", 18L),
    ADMISSAO_SOLICITAR_CARTA("04.99", "Admissão, solicitar carta de transferência", 19L),
    
    // Categoria 05 (Em processo de admissão)
    ADMITENDO_MENOR_BATIZADO("05.01", "Admitendo, menor batizado a admitir juntamente com pais ou responsáveis", 20L),
    ADMITENDO_MENOR_AGUARDANDO_BATISMO("05.10", "Admitendo, menor aguardando batismo infantil", 21L),
    ADMITENDO_NAO_PRESBITERIANO("05.15", "Admitendo, não presbiteriano aguardando votos de membresia", 22L),
    ADMITENDO_ADULTO_AGUARDANDO_PROFISSAO("05.50", "Admitendo, adulto/jovem aguardando profissão de fé", 23L),
    ADMITENDO_ADULTO_AGUARDANDO_PROFISSAO_BATISMO("05.51", "Admitendo, adulto/jovem aguardando profissão de fé e batismo", 24L),
    ADMITENDO_AGUARDANDO_EXAME("05.59", "Admitendo, aguardando exame para profissão de fé", 25L),
    ADMITENDO_AGUARDANDO_CASAMENTO("05.60", "Admitendo, aguardando casamento com membro da Igreja", 26L),
    ADMITENDO_AGUARDANDO_RESOLUCAO("05.70", "Admitendo, aguardando resolução pendência", 27L),
    ADMITENDO_AGUARDANDO_ENTREVISTA("05.90", "Admitendo, aguardando entrevista", 28L),
    
    // Categoria 06 (Em avaliação para admissão)
    POSSIVEL_ADMISSAO_CATEQUIZACAO("06.01", "Possível admissão: em catequização", 29L),
    POSSIVEL_ADMISSAO_FILHOS_PAIS("06.94", "Possível admissão: filhos de pais em catequização", 30L),
    POSSIVEL_ADMISSAO_GESTACAO("06.95", "Possível admissão: gestação", 31L),
    POSSIVEL_ADMISSAO_SOBRESTADA("06.96", "Possível admissão: admissão sobrestada (pedido, impedim. ou discord. CFW)", 32L),
    POSSIVEL_ADMISSAO_BATISMO_SOBRESTADO("06.97", "Possível admissão: batismo de menor sobrestado (credobatismo)", 33L),
    POSSIVEL_ADMISSAO_AVALIACAO("06.98", "Possível admissão: em avaliação", 34L),
    POSSIVEL_ADMISSAO_FICHA_CADASTRAL("06.99", "Possível admissão: aguardando ficha cadastral", 35L),
    
    // Categoria 07 (Pastor congregante)
    PASTOR_CONGREGANTE("07", "Pastor congregante", 36L),
    
    // Categoria 08 (Agregado não membro)
    AGREGADO_NAO_MEMBRO("08", "Agregado não membro (p.ex. familiar frequente)", 37L),
    
    // Categoria 09 (Pessoa fictícia)
    PESSOA_FICTICIA("09", "Pessoa fictícia (sistema)", 38L),
    
    // Categoria 40 (Visitantes frequentes)
    VISITANTE_RECORRENTE("42", "Visitante recorrente EBD/GF/3aIdade/EnglishBibleStudy/Youtube", 39L),
    VISITANTE_FREQUENTE("43", "Visitante frequente", 40L),
    VISITANTE_FREQUENTE_MENOR("44", "Visitante frequente (menor de idade)", 41L),
    VISITANTE_FREQUENTE_SEM_INTENCAO("45", "Visitante frequente, mas sem intenção de admissão", 42L),
    VISITANTE_EVENTO("46", "Visitante evento (AcampUMP, Conferência etc.)", 43L),
    
    // Categoria 48 (Aparece na contabilidade)
    APARECE_NA_CONTABILIDADE("48", "Aparece na contabilidade", 44L),
    
    // Categoria 49 (Possível admissão: gestação)
    POSSIVEL_ADMISSAO_GESTACAO_SIGILO("49", "Possível admissão: gestação (mantida em sigilo, temporariamente)", 45L),
    
    // Categoria 51 (Missionários apoiados)
    MISSIONARIOS_APOIADOS("51", "Missionários apoiados", 46L),
    
    // Categoria 52 (Missionários eventualmente auxiliados)
    MISSIONARIOS_EVENTUALMENTE_AUXILIADOS("52", "Missionários eventualmente auxiliados", 47L),
    
    // Categoria 80 (Oficiais da IPB)
    OFICIAIS_DA_IPB("80", "Oficiais da IPB", 48L),
    
    // Categoria 90 (Visitantes)
    VISITANTE_OCASIONAL("90", "Visitante ocasional periódico", 49L),
    
    // Categoria 91 (Ex-membro da Igreja)
    EX_MEMBRO_DA_IGREJA("91", "Ex-membro da Igreja", 50L),
    
    // Categoria 95 (Visitante)
    VISITANTE("95", "Visitante", 51L),
    
    // Categoria 97 (Relacionamento profissional)
    RELACIONAMENTO_PROFISSIONAL("97", "Relacionamento profissional", 52L),
    
    // Categoria 98 (Organização)
    ORGANIZACAO("98", "Organização", 53L),
    
    // Categoria 99 (Pessoa referenciada)
    PESSOA_REFERENCIADA("99", "Pessoa referenciada (sistema)", 54L);

    private final String codigo;
    private final String nome;
    private final Long id;
    
    SubcategoriaEnum(String codigo, String nome, Long id) {
        this.codigo = codigo;
        this.nome = nome;
        this.id = id;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public String getNome() {
        return nome;
    }
    
    public Long getId() {
        return id;
    }
    
    /**
     * Retorna a categoria à qual esta subcategoria pertence
     * @return CategoriaEnum correspondente
     */
    public CategoriaEnum getCategoria() {
        return CategoriaEnum.fromCodigoSubcategoria(this.codigo);
    }
    
    /**
     * Retorna todas as subcategorias de uma categoria específica
     * @param categoria A categoria para filtrar
     * @return Array de subcategorias da categoria informada
     */
    public static SubcategoriaEnum[] getByCategoria(CategoriaEnum categoria) {
        return java.util.Arrays.stream(values())
            .filter(sub -> {
                CategoriaEnum subCategoria = sub.getCategoria();
                return subCategoria != null && subCategoria == categoria;
            })
            .toArray(SubcategoriaEnum[]::new);
    }
    
    public static SubcategoriaEnum fromCodigo(String codigo) {
        for (SubcategoriaEnum subcategoria : values()) {
            if (subcategoria.codigo.equals(codigo)) {
                return subcategoria;
            }
        }
        throw new IllegalArgumentException("Código de subcategoria não encontrado: " + codigo);
    }
    
    public static SubcategoriaEnum fromNome(String nome) {
        for (SubcategoriaEnum subcategoria : values()) {
            if (subcategoria.nome.equals(nome)) {
                return subcategoria;
            }
        }
        throw new IllegalArgumentException("Nome de subcategoria não encontrado: " + nome);
    }
    
    public static SubcategoriaEnum fromId(Long id) {
        for (SubcategoriaEnum subcategoria : values()) {
            if (subcategoria.id.equals(id)) {
                return subcategoria;
            }
        }
        return CATEGORIA_DESCONHECIDA;
    }
}
