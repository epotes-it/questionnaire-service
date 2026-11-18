package com.athron.questionnaire;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.athron.questionnaire.QuestionnaireModels.*;

@RestController
@RequestMapping("/api/v1/questionnaires")
class QuestionnaireController {
    private final QuestionnaireService service;

    QuestionnaireController(QuestionnaireService service) {
        this.service = service;
    }

    @GetMapping
    List<Summary> list(@RequestAttribute String accountId,
                       @RequestParam(defaultValue = "0") int offset,
                       @RequestParam(defaultValue = "50") int limit) {
        return service.list(accountId, offset, limit);
    }

    @PostMapping
    ResponseEntity<View> create(@RequestAttribute String accountId, @Valid @RequestBody Write body) {
        View view = service.create(accountId, body);
        return ResponseEntity.created(URI.create("/api/v1/questionnaires/" + view.id())).body(view);
    }

    @GetMapping("/{id}")
    View get(@RequestAttribute String accountId, @PathVariable UUID id) {
        return service.get(accountId, id);
    }

    @PutMapping("/{id}")
    View update(@RequestAttribute String accountId, @PathVariable UUID id, @Valid @RequestBody Write body) {
        return service.update(accountId, id, body);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@RequestAttribute String accountId, @PathVariable UUID id) {
        service.delete(accountId, id);
        return ResponseEntity.noContent().build();
    }
}
