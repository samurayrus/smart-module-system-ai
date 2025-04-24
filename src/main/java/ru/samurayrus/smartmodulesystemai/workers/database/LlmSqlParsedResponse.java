package ru.samurayrus.smartmodulesystemai.workers.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmSqlParsedResponse {
    private String sqlQuery;
    private boolean hasSql;
    private String userMessage;
}
