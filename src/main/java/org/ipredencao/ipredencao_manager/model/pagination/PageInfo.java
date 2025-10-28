package org.ipredencao.ipredencao_manager.model.pagination;

public class PageInfo {
    private int limit;
    private int offset;
    private long total;
    
    public PageInfo() {}
    
    public PageInfo(int limit, int offset, long total) {
        this.limit = limit;
        this.offset = offset;
        this.total = total;
    }
    
    // Getters
    public int getLimit() {
        return limit;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
}

