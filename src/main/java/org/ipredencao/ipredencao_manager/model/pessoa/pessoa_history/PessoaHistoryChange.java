package org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history;

public class PessoaHistoryChange {
    private String attribute;
    private Object oldValue;
    private Object newValue;

    public PessoaHistoryChange() {}

    public PessoaHistoryChange(String attribute, Object oldValue, Object newValue) {
        this.attribute = attribute;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
}
