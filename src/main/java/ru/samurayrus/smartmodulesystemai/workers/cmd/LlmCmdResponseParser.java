package ru.samurayrus.smartmodulesystemai.workers.cmd;

import ru.samurayrus.smartmodulesystemai.workers.database.LlmSqlParsedResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LlmCmdResponseParser {
    //TODO: Из воркеров вынести тэги начала и окончания в env, чтобы оптимально собирать и отправлять в gui
    private static final Pattern CMD_PATTERN =
            Pattern.compile("<CMD_START>(.+?)<CMD_END>", Pattern.DOTALL);

    public LlmCmdParsedResponse parseResponse(String llmResponse) {
        LlmCmdParsedResponse response = new LlmCmdParsedResponse();

        Matcher cmdMatcher = CMD_PATTERN.matcher(llmResponse);
        if (cmdMatcher.find()) {
            // Получаем команду и удаляем все переносы строк ( Чтобы в CMD не летели многострочные запросы, которые ломают вызовы)
            String cmd = cmdMatcher.group(1).replaceAll("\\s+", " ");//.trim();
            response.setCmdQuery(cmd);
            response.setHasCmd(true);

            // Вырезаем командную часть чтобы получить только комментарии
            response.setUserMessage(llmResponse.replace(cmdMatcher.group(0), "").trim());
        } else {
            response.setHasCmd(false);
            response.setUserMessage(llmResponse.trim());
        }

        return response;
    }
}
