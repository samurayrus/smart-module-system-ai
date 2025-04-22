package ru.samurayrus.smartmodulesystemai.databases;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedResponse {
    private String sqlQuery;
    private boolean hasSql;
    private String userMessage;
}
