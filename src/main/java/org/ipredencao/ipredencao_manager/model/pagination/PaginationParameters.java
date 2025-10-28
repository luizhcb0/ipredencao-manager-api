package org.ipredencao.ipredencao_manager.model.pagination;

public class PaginationParameters {
    private Integer limit;
    private Integer offset;
    
    // Constantes
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 250;
    public static final int MIN_LIMIT = 1;
    public static final int DEFAULT_OFFSET = 0;
    
    public PaginationParameters() {}
    
    public PaginationParameters(Integer limit, Integer offset) {
        this.limit = limit;
        this.offset = offset;
    }
    
    // Getters e Setters
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Integer getOffset() {
        return offset;
    }
    
    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}

