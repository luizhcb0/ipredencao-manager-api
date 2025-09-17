package org.ipredencao.ipredencao_manager.model.pessoa;

public enum CategoriaEnum {
    PASTORES("00", "Pastores"),
    MEMBROS_COMUNGANTES("01", "Membros comungantes"),
    MEMBROS_COMUNGANTES_ROL_APARTE("02", "Membros comungantes (rol à parte)"),
    MEMBROS_NAO_COMUNGANTES("03", "Membros não comungantes"),
    ADMISSAO("04", "Admissão"),
    EM_PROCESSO_ADMISSAO("05", "Em processo de admissão"),
    EM_AVALIACAO_ADMISSAO("06", "Em avaliação para admissão"),
    PASTOR_CONGREGANTE("07", "Pastor congregante"),
    FREQUENTADOR("08", "Frequentador"),
    CONTATO("09", "Contato"),
    PESSOA_CADASTRADA("10", "Pessoa cadastrada"),
    PESSOA_FALECIDA("11", "Pessoa falecida"),
    PESSOA_DESLIGADA("12", "Pessoa desligada");
    
    private final String codigo;
    private final String nome;
    
    CategoriaEnum(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public String getNome() {
        return nome;
    }
    
    public static CategoriaEnum fromCodigo(String codigo) {
        for (CategoriaEnum categoria : values()) {
            if (categoria.codigo.equals(codigo)) {
                return categoria;
            }
        }
        throw new IllegalArgumentException("Categoria não encontrada para o código: " + codigo);
    }
    
    /**
     * Retorna a categoria baseada no código da subcategoria
     * @param codigoSubcategoria Código da subcategoria (ex: "02.01", "03.99")
     * @return CategoriaEnum correspondente ou null se não encontrado
     */
    public static CategoriaEnum fromCodigoSubcategoria(String codigoSubcategoria) {
        if (codigoSubcategoria == null || codigoSubcategoria.isEmpty()) {
            return null;
        }
        
        // Extrai os primeiros 2 dígitos do código da subcategoria
        String codigoCategoria = codigoSubcategoria.length() >= 2 ? 
            codigoSubcategoria.substring(0, 2) : codigoSubcategoria;
        
        try {
            return fromCodigo(codigoCategoria);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
