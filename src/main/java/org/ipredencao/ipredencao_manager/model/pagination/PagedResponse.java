package org.ipredencao.ipredencao_manager.model.pagination;

import java.util.List;

public class PagedResponse<T> {
    private List<T> data;
    private PageInfo page;
    
    public PagedResponse() {}
    
    public PagedResponse(List<T> data, PageInfo page) {
        this.data = data;
        this.page = page;
    }
    
    // Getters e Setters
    public List<T> getData() {
        return data;
    }
    
    public void setData(List<T> data) {
        this.data = data;
    }
    
    public PageInfo getPage() {
        return page;
    }
    
    public void setPage(PageInfo page) {
        this.page = page;
    }
}

