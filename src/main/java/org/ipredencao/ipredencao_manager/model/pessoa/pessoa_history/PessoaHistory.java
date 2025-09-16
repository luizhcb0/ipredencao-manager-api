package org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history;

import org.joda.time.DateTime;
import java.util.List;
import java.util.ArrayList;

public class PessoaHistory {
    private DateTime updatedAt;
    private Long updatedByUserId;
    private String updatedByUserName;
    private List<PessoaHistoryChange> changes;

    public PessoaHistory() {
        this.changes = new ArrayList<>();
    }

    public PessoaHistory(DateTime updatedAt, Long updatedByUserId) {
        this.updatedAt = updatedAt;
        this.updatedByUserId = updatedByUserId;
        this.changes = new ArrayList<>();
    }

    public PessoaHistory(DateTime updatedAt, Long updatedByUserId, String updatedByUserName) {
        this.updatedAt = updatedAt;
        this.updatedByUserId = updatedByUserId;
        this.updatedByUserName = updatedByUserName;
        this.changes = new ArrayList<>();
    }

    public PessoaHistory(DateTime updatedAt, Long updatedByUserId, List<PessoaHistoryChange> changes) {
        this.updatedAt = updatedAt;
        this.updatedByUserId = updatedByUserId;
        this.changes = changes != null ? changes : new ArrayList<>();
    }

    public PessoaHistory(DateTime updatedAt, Long updatedByUserId, String updatedByUserName, List<PessoaHistoryChange> changes) {
        this.updatedAt = updatedAt;
        this.updatedByUserId = updatedByUserId;
        this.updatedByUserName = updatedByUserName;
        this.changes = changes != null ? changes : new ArrayList<>();
    }

    public void addChange(PessoaHistoryChange change) {
        if (this.changes == null) {
            this.changes = new ArrayList<>();
        }
        this.changes.add(change);
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public String getUpdatedByUserName() {
        return updatedByUserName;
    }

    public void setUpdatedByUserName(String updatedByUserName) {
        this.updatedByUserName = updatedByUserName;
    }

    public List<PessoaHistoryChange> getChanges() {
        return changes;
    }

    public void setChanges(List<PessoaHistoryChange> changes) {
        this.changes = changes;
    }

}