CREATE TABLE questionnaire (
    id uuid PRIMARY KEY,
    account_id varchar(255) NOT NULL,
    title varchar(200) NOT NULL
);
CREATE INDEX questionnaire_account_idx ON questionnaire (account_id, id);

CREATE TABLE question (
    id uuid PRIMARY KEY,
    questionnaire_id uuid NOT NULL REFERENCES questionnaire(id) ON DELETE CASCADE,
    prompt varchar(2000) NOT NULL,
    type varchar(20) NOT NULL CHECK (type IN ('SINGLE_SELECT', 'MULTI_SELECT', 'FREE_TEXT')),
    position integer NOT NULL CHECK (position >= 0),
    UNIQUE (questionnaire_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE question_option (
    id uuid PRIMARY KEY,
    question_id uuid NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    label varchar(500) NOT NULL,
    position integer NOT NULL CHECK (position >= 0),
    UNIQUE (question_id, position) DEFERRABLE INITIALLY DEFERRED
);
