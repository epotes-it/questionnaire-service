package com.athron.questionnaire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.athron.questionnaire.QuestionnaireModels.*;

@Service
class QuestionnaireService {
    private final JdbcTemplate jdbc;

    QuestionnaireService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Summary> list(String accountId, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) {
            throw invalid("Offset must be nonnegative and limit must be between 1 and 100");
        }
        return jdbc.query("SELECT id, title FROM questionnaire WHERE account_id = ? ORDER BY id LIMIT ? OFFSET ?",
                (row, index) -> new Summary(row.getObject("id", UUID.class), row.getString("title")),
                accountId, limit, offset);
    }

    @Transactional
    View create(String accountId, Write body) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO questionnaire (id, account_id, title) VALUES (?, ?, ?)", id, accountId, body.title());
        return new View(id, body.title(), saveQuestions(id, body.questions()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    View get(String accountId, UUID id) {
        return new View(id, title(accountId, id, false), readQuestions(id));
    }

    @Transactional
    View update(String accountId, UUID id, Write body) {
        title(accountId, id, true);
        jdbc.update("UPDATE questionnaire SET title = ? WHERE id = ?", body.title(), id);
        return new View(id, body.title(), saveQuestions(id, body.questions()));
    }

    @Transactional
    void delete(String accountId, UUID id) {
        if (jdbc.update("DELETE FROM questionnaire WHERE id = ? AND account_id = ?", id, accountId) == 0) {
            throw notFound();
        }
    }

    private String title(String accountId, UUID id, boolean forUpdate) {
        String sql = "SELECT title FROM questionnaire WHERE id = ? AND account_id = ?";
        return jdbc.queryForList(forUpdate ? sql + " FOR UPDATE" : sql, String.class, id, accountId)
                .stream().findFirst().orElseThrow(QuestionnaireService::notFound);
    }

    private List<QuestionView> readQuestions(UUID id) {
        List<QuestionView> questions = jdbc.query(
                "SELECT id, prompt, type FROM question WHERE questionnaire_id = ? ORDER BY position",
                (row, index) -> new QuestionView(row.getObject("id", UUID.class), row.getString("prompt"),
                        QuestionType.valueOf(row.getString("type")), List.of()), id);
        return questions.stream().map(question ->
                new QuestionView(question.id(), question.prompt(), question.type(), readOptions(question.id()))).toList();
    }

    private List<OptionView> readOptions(UUID id) {
        return jdbc.query("SELECT id, label FROM question_option WHERE question_id = ? ORDER BY position",
                (row, index) -> new OptionView(row.getObject("id", UUID.class), row.getString("label")), id);
    }

    private List<QuestionView> saveQuestions(UUID questionnaireId, List<QuestionWrite> questions) {
        Set<UUID> omitted = new HashSet<>(jdbc.queryForList(
                "SELECT id FROM question WHERE questionnaire_id = ?", UUID.class, questionnaireId));
        List<QuestionView> saved = new ArrayList<>(questions.size());
        for (QuestionWrite question : questions) {
            List<OptionWrite> options = question.options() == null ? List.of() : question.options();
            if (question.type() == QuestionType.FREE_TEXT && !options.isEmpty()) {
                throw invalid("Free-text questions cannot have options");
            }
            if (question.type() != QuestionType.FREE_TEXT && options.isEmpty()) {
                throw invalid("Selection questions require options");
            }
            UUID id = question.id();
            int position = saved.size();
            if (id == null) {
                id = UUID.randomUUID();
                jdbc.update("INSERT INTO question (id, questionnaire_id, prompt, type, position) VALUES (?, ?, ?, ?, ?)",
                        id, questionnaireId, question.prompt(), question.type().name(), position);
            } else {
                requireChild(omitted, id, "question");
                jdbc.update("UPDATE question SET prompt = ?, type = ?, position = ? WHERE id = ?",
                        question.prompt(), question.type().name(), position, id);
            }
            saved.add(new QuestionView(id, question.prompt(), question.type(), saveOptions(id, options)));
        }
        omitted.forEach(id -> jdbc.update("DELETE FROM question WHERE id = ?", id));
        return saved;
    }

    private List<OptionView> saveOptions(UUID questionId, List<OptionWrite> options) {
        Set<UUID> omitted = new HashSet<>(jdbc.queryForList(
                "SELECT id FROM question_option WHERE question_id = ?", UUID.class, questionId));
        List<OptionView> saved = new ArrayList<>(options.size());
        for (OptionWrite option : options) {
            UUID id = option.id();
            int position = saved.size();
            if (id == null) {
                id = UUID.randomUUID();
                jdbc.update("INSERT INTO question_option (id, question_id, label, position) VALUES (?, ?, ?, ?)",
                        id, questionId, option.label(), position);
            } else {
                requireChild(omitted, id, "option");
                jdbc.update("UPDATE question_option SET label = ?, position = ? WHERE id = ?",
                        option.label(), position, id);
            }
            saved.add(new OptionView(id, option.label()));
        }
        omitted.forEach(id -> jdbc.update("DELETE FROM question_option WHERE id = ?", id));
        return saved;
    }

    private static void requireChild(Set<UUID> remaining, UUID id, String resource) {
        if (!remaining.remove(id)) {
            throw invalid("Invalid or repeated " + resource + " identifier");
        }
    }

    private static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Questionnaire not found");
    }
}
