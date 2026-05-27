package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.pessoa.PersonNote;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.DSLContext;
import org.jooq.Record;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PERSON_NOTE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class PersonNoteRepository {

    @Autowired
    private DSLContext dsl;

    public List<PersonNote> findByPessoaId(Long pessoaId) {
        return dsl.select()
                .from(PERSON_NOTE)
                .where(PERSON_NOTE.PERSON_ID.eq(pessoaId))
                .orderBy(PERSON_NOTE.ADDED_AT.asc())
                .fetch(this::fromRepository);
    }

    public PersonNote findById(Long id) {
        Record record = dsl.select()
                .from(PERSON_NOTE)
                .where(PERSON_NOTE.ID.eq(id))
                .fetchOne();
        return record != null ? fromRepository(record) : null;
    }

    public PersonNote insert(Long pessoaId, String content, Long updatedBy) {
        Record record = dsl.insertInto(PERSON_NOTE)
                .set(PERSON_NOTE.PERSON_ID, pessoaId)
                .set(PERSON_NOTE.CONTENT, content)
                .set(PERSON_NOTE.UPDATED_BY, updatedBy)
                .returning()
                .fetchOne();
        return fromRepository(record);
    }

    public PersonNote update(Long id, String content, Long updatedBy) {
        dsl.update(PERSON_NOTE)
                .set(PERSON_NOTE.CONTENT, content)
                .set(PERSON_NOTE.UPDATED_BY, updatedBy)
                .where(PERSON_NOTE.ID.eq(id))
                .execute();
        return findById(id);
    }

    public void delete(Long id) {
        dsl.deleteFrom(PERSON_NOTE)
                .where(PERSON_NOTE.ID.eq(id))
                .execute();
    }

    private PersonNote fromRepository(Record record) {
        PersonNote note = new PersonNote();
        note.setId(record.get(PERSON_NOTE.ID));
        note.setPessoaId(record.get(PERSON_NOTE.PERSON_ID));
        note.setContent(record.get(PERSON_NOTE.CONTENT));
        note.setAddedAt(DateTimeHelper.fromDb(record.get(PERSON_NOTE.ADDED_AT)));
        note.setUpdatedAt(DateTimeHelper.fromDb(record.get(PERSON_NOTE.UPDATED_AT)));
        note.setUpdatedBy(record.get(PERSON_NOTE.UPDATED_BY));
        return note;
    }
}
