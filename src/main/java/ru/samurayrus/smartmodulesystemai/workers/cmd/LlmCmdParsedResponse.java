package ru.samurayrus.smartmodulesystemai.workers.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmCmdParsedResponse {
    private String cmdQuery;
    private boolean hasCmd;
    private String userMessage;
}
