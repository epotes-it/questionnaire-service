# Questionnaire service

Spring Boot service for questionnaires owned by an Athron account. Nginx validates the session and forwards the authenticated subject. The service checks the gateway API key and scopes every operation to that subject.

| Method | Path | Success |
|---|---|---|
| GET | /api/v1/questionnaires?offset=0&limit=50 | 200 |
| POST | /api/v1/questionnaires | 201 |
| GET | /api/v1/questionnaires/{id} | 200 |
| PUT | /api/v1/questionnaires/{id} | 200 |
| DELETE | /api/v1/questionnaires/{id} | 204 |

POST and PUT accept a title and ordered questions. Each question has a prompt, a type, and options when applicable. Option objects have a label. Supported types are SINGLE_SELECT, MULTI_SELECT, and FREE_TEXT. Selection questions require options. Text questions accept an empty or omitted options array.

PUT replaces the questionnaire contents atomically. Supply IDs returned by the API to keep existing questions or options, omit IDs for new items, and omit existing items to delete them. Array order determines display order. A questionnaire owned by another account returns 404. Participant responses and scoring are outside this API.

Build with Java 21 and mvn package. At runtime set DB_URL, DB_USERNAME, DB_PASSWORD, and API_KEY. Flyway creates the schema in the service database at startup.
