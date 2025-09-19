package org.ipredencao.ipredencao_manager.model.pessoa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CategoriaEnum {
    // Categorias do agregador Pastor (id=1)
    PASTOR_DA_IGREJA(1L, "Pastor da igreja", 1L),
    PASTOR_CONGREGANTE(2L, "Pastor congregante", 1L),
    
    // Categorias do agregador Membro comungante (id=2)
    MEMBRO_COMUNGANTE(3L, "Membro comungante", 2L),
    
    // Categorias do agregador Membro não comungante (id=3)
    AGUARDANDO_PROFISSAO(4L, "Aguardando profissão de fé", 3L),
    AGUARDANDO_EXAME(5L, "Aguardando exame para profissão de fé", 3L),
    MEMBRO_NAO_COMUNGANTE_ESPECIAL(7L, "Especial, não requer profissão de fé", 3L),
    MEMBRO_NAO_COMUNGANTE(8L, "Membro não comungante", 3L),
    MEMBRO_NAO_COMUNGANTE_IDADE_PROFISSAO(9L, "Em idade para profissão de fé", 3L),
    
    // Categorias do agregador Rol à parte (id=4)
    MEMBRO_EM_TRANSITO(10L, "Membro em trânsito", 4L),
    MEMBRO_AUSENTE(11L, "Membro ausente", 4L),
    MEMBRO_A_TRANSFERIR(12L, "Membro a transferir", 4L),
    MEMBRO_NAO_LOCALIZADO(13L, "Membro não localizado ou pedido de desligamento", 4L),
    MEMBRO_SOB_DISCIPLINA(14L, "Membro sob disciplina", 4L),
    
    // Categorias do agregador Admitendo (id=5)
    AGUARDANDO_CARTA_TRANSFERENCIA(15L, "Aguardando carta de transferência", 5L),
    SOLICITAR_CARTA_TRANSFERENCIA(16L, "Solicitar carta de transferência", 5L),
    AGUARDANDO_BATISMO_INFANTIL(17L, "Aguardando batismo infantil", 5L),
    AGUARDANDO_VOTOS_MEMBRESIA(18L, "Aguardando votos de membresia", 5L),
    AGUARDANDO_PROFISSAO_FE(19L, "Aguardando profissão de fé", 5L),
    AGUARDANDO_PROFISSAO_FE_BATISMO(20L, "Aguardando profissão de fé e batismo", 5L),
    AGUARDANDO_EXAME_PROFISSAO_FE(21L, "Aguardando exame para profissão de fé", 5L),
    AGUARDANDO_CASAMENTO_MEMBRO(22L, "Aguardando casamento com membro da Igreja", 5L),
    AGUARDANDO_RESOLUCAO_PENDENCIA(23L, "Aguardando resolução pendência", 5L),
    AGUARDANDO_ENTREVISTA(24L, "Aguardando entrevista", 5L),
    EM_CATEQUIZACAO(25L, "Em catequização", 5L),
    ADMISSAO_SOBRESTADA(26L, "Admissão sobrestada (pedido, impedim. ou discord. CFW)", 5L),
    BATISMO_MENOR_SOBRESTADO(27L, "Admissão: batismo de menor sobrestado (credobatismo)", 5L),
    
    // Categorias do agregador Transferido (id=6)
    TRANSFERIDO_OUTRA_IGREJA(28L, "Transferido para outra igreja", 6L),
    
    // Categorias do agregador Excluido (id=7)
    EXCLUIDO_A_PEDIDO(29L, "Excluído a pedido", 7L),
    EXCLUIDO_POR_ABANDONO(30L, "Excluído por abandono", 7L),
    EXCLUIDO_POR_DISCIPLINA(31L, "Excluído por disciplina", 7L),
    EXCLUIDO_POR_FALECIMENTO(32L, "Excluído por falecimento", 7L),
    
    // Categorias do agregador Missionário (id=8)
    MISSIONARIO_APOIADO(33L, "Missionário apoiado", 8L),
    MISSIONARIO_EVENTUALMENTE_AUXILIADO(34L, "Missionário eventualmente auxiliado", 8L),
    
    // Categorias do agregador Possível admissão: gestação (id=9)
    GESTACAO(35L, "Gestação", 9L),
    GESTACAO_SIGILO_TEMPORARIO(36L, "Gestação mantida em sigilo temporariamente", 9L),
    
    // Categorias do agregador Pessoa referenciada (id=10)
    AGREGADO_FAMILIAR(37L, "Agregado ou familiar", 10L);

    private final Long id;
    private final String nome;
    private final Long agregadorCategoriaId;
    
    CategoriaEnum(Long id, String nome, Long agregadorCategoriaId) {
        this.id = id;
        this.nome = nome;
        this.agregadorCategoriaId = agregadorCategoriaId;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public Long getAgregadorCategoriaId() {
        return agregadorCategoriaId;
    }
    
    /**
     * Retorna o agregador de categoria ao qual esta categoria pertence
     * @return AgregadorCategoriaEnum correspondente
     */
    public AgregadorCategoriaEnum getAgregadorCategoria() {
        return AgregadorCategoriaEnum.fromId(this.agregadorCategoriaId);
    }
    
    /**
     * Retorna todas as categorias de um agregador específico
     * @param agregadorCategoria O agregador para filtrar
     * @return Array de categorias do agregador informado
     */
    public static CategoriaEnum[] getByAgregadorCategoria(AgregadorCategoriaEnum agregadorCategoria) {
        return java.util.Arrays.stream(values())
            .filter(categoria -> categoria.agregadorCategoriaId.equals(agregadorCategoria.getId()))
            .toArray(CategoriaEnum[]::new);
    }
    
    @JsonCreator
    public static CategoriaEnum fromId(Long id) {
        for (CategoriaEnum categoria : values()) {
            if (categoria.id.equals(id)) {
                return categoria;
            }
        }
        throw new IllegalArgumentException("Categoria não encontrada para o ID: " + id);
    }
    
    public static CategoriaEnum fromName(String nome) {
        for (CategoriaEnum categoria : values()) {
            if (categoria.nome.equals(nome)) {
                return categoria;
            }
        }
        throw new IllegalArgumentException("Categoria não encontrada para o nome: " + nome);
    }
}