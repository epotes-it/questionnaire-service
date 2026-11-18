package com.athron.questionnaire;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class QuestionnaireModels {
    enum QuestionType { SINGLE_SELECT, MULTI_SELECT, FREE_TEXT }

    record Write(@NotBlank @Size(max = 200) String title,
                 @NotNull @Size(max = 200) List<@NotNull @Valid QuestionWrite> questions) {}
    record QuestionWrite(UUID id, @NotBlank @Size(max = 2000) String prompt,
                         @NotNull QuestionType type,
                         @Size(max = 100) List<@NotNull @Valid OptionWrite> options) {}
    record OptionWrite(UUID id, @NotBlank @Size(max = 500) String label) {}

    record Summary(UUID id, String title) {}
    record View(UUID id, String title, List<QuestionView> questions) {}
    record QuestionView(UUID id, String prompt, QuestionType type, List<OptionView> options) {}
    record OptionView(UUID id, String label) {}
}
