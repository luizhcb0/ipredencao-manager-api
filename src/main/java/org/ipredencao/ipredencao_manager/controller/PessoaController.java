package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pessoa.PersonNote;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistory;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistoryResponse;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.ipredencao.ipredencao_manager.repository.PersonNoteRepository;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {

    private final PessoaService pessoaService;
    private final PersonNoteRepository personNoteRepository;

    public PessoaController(PessoaService pessoaService, PersonNoteRepository personNoteRepository) {
        this.pessoaService = pessoaService;
        this.personNoteRepository = personNoteRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<Pessoa> createPerson(@RequestBody Pessoa pessoa) {
        return ResponseEntity.ok(pessoaService.create(pessoa));
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<Pessoa> uploadPhoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) throws IOException {
        Pessoa pessoa = pessoaService.findById(id);
        return ResponseEntity.ok(pessoaService.savePhoto(pessoa, foto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<Pessoa> findById(@PathVariable Long id) {
        return ResponseEntity.ok(pessoaService.findById(id));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<PagedResponse<Pessoa>> searchPeople(@RequestBody PessoaQuery query) {
        return ResponseEntity.ok(pessoaService.findPaginated(query));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<Pessoa> updatePerson(@PathVariable Long id, @RequestBody Pessoa pessoa) {
        pessoa.setId(id);
        return ResponseEntity.ok(pessoaService.update(pessoa));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<PessoaHistoryResponse> findHistoryById(@PathVariable Long id) {
        List<PessoaHistory> history = pessoaService.findHistoryById(id);
        return ResponseEntity.ok(new PessoaHistoryResponse(id, history));
    }

    @PostMapping("/{id}/relacionamento")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<Relacionamento> createRelationship(@PathVariable Long id, @RequestBody Relacionamento relacionamento) {
        return ResponseEntity.ok(pessoaService.createRelationship(id, relacionamento));
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<List<PersonNote>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(personNoteRepository.findByPessoaId(id));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<PersonNote> createNote(@PathVariable Long id, @RequestBody PersonNote note) {
        return ResponseEntity.ok(personNoteRepository.insert(id, note.getContent(), note.getUpdatedBy()));
    }

    @PutMapping("/{id}/notes/{noteId}")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<PersonNote> updateNote(@PathVariable Long id, @PathVariable Long noteId, @RequestBody PersonNote note) {
        PersonNote existing = personNoteRepository.findById(noteId);
        if (existing == null || !existing.getPessoaId().equals(id)) {
            throw new NoSuchElementException("Note not found");
        }
        return ResponseEntity.ok(personNoteRepository.update(noteId, note.getContent(), note.getUpdatedBy()));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id, @PathVariable Long noteId) {
        PersonNote existing = personNoteRepository.findById(noteId);
        if (existing == null || !existing.getPessoaId().equals(id)) {
            throw new NoSuchElementException("Note not found");
        }
        personNoteRepository.delete(noteId);
        return ResponseEntity.noContent().build();
    }
}
