package ru.samurayrus.smartmodulesystemai.workers.database;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LlmSqlResponseParser {

    private static final Pattern SQL_PATTERN =
            Pattern.compile("<SQL_START>(.+?)<SQL_END>", Pattern.DOTALL);

    public LlmSqlParsedResponse parseResponse(String llmResponse) {
        LlmSqlParsedResponse response = new LlmSqlParsedResponse();

        // Ищем SQL запрос
        Matcher sqlMatcher = SQL_PATTERN.matcher(llmResponse);
        if (sqlMatcher.find()) {
            response.setSqlQuery(sqlMatcher.group(1).trim());
            response.setHasSql(true);

            // Вырезаем SQL часть чтобы получить только комментарии
            response.setUserMessage(llmResponse.replace(sqlMatcher.group(0), "").trim());
        } else {
            response.setHasSql(false);
            response.setUserMessage(llmResponse.trim());
        }

        return response;
    }

}
