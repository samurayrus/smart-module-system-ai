package ru.samurayrus.smartmodulesystemai.workers.cmd;

import ru.samurayrus.smartmodulesystemai.workers.database.LlmSqlParsedResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LlmCmdResponseParser {

    private static final Pattern CMD_PATTERN =
            Pattern.compile("<CMD_START>\\n(.+?)\\n<CMD_END>", Pattern.DOTALL);

    public LlmCmdParsedResponse parseResponse(String llmResponse) {
        LlmCmdParsedResponse response = new LlmCmdParsedResponse();

        // Ищем SQL запрос
        Matcher sqlMatcher = CMD_PATTERN.matcher(llmResponse);
        if (sqlMatcher.find()) {
            response.setCmdQuery(sqlMatcher.group(1).trim());
            response.setHasCmd(true);

            // Вырезаем SQL часть чтобы получить только комментарии
            response.setUserMessage(llmResponse.replace(sqlMatcher.group(0), "").trim());
        } else {
            response.setHasCmd(false);
            response.setUserMessage(llmResponse.trim());
        }

        return response;
    }

}
