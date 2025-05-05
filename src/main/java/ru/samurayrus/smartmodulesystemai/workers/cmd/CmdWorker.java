package ru.samurayrus.smartmodulesystemai.workers.cmd;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.samurayrus.smartmodulesystemai.gui.ContextStorage;
import ru.samurayrus.smartmodulesystemai.workers.WorkerEventDataBus;
import ru.samurayrus.smartmodulesystemai.workers.WorkerListener;
import ru.samurayrus.smartmodulesystemai.utils.Command;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Воркер отвечает за НЕБЕЗОПАСНУЮ работу LLM с CMD. Решение для смельчаков.
 * Зато у нейронки появляется новый функционал ;)
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.modules.cmd-worker", name = "enabled", havingValue = "true")
public class CmdWorker implements WorkerListener {
    private final LlmCmdResponseParser llmCmdResponseParser = new LlmCmdResponseParser();
    private final WorkerEventDataBus workerEventDataBus;
    private final ContextStorage contextStorage;

    @Autowired
    public CmdWorker(ContextStorage contextStorage, WorkerEventDataBus workerEventDataBus) {
        this.workerEventDataBus = workerEventDataBus;
        this.contextStorage = contextStorage;
    }

    @PostConstruct
    void registerWorker() {
        log.info("CmdWorker init registration as worker...");
        workerEventDataBus.registerWorker(this);

        Map<String, String> tagsMag = new HashMap<>();
        tagsMag.put("<CMD_START>", "<span style='color:green;'>&lt;CMD_START&gt;");
        tagsMag.put("<CMD_END>", "&lt;CMD_END&gt;</span>");
        contextStorage.addReplacerSpecialTagFromWorkerToGuiMessages(tagsMag);
    }

    @Override
    public boolean callWorker(String content, boolean toolMode) {
        LlmCmdParsedResponse llmCmdParsedResponse = llmCmdResponseParser.parseResponse(content);

        if (llmCmdParsedResponse.isHasCmd()) {
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", "[Запрос к CMD]: " + llmCmdParsedResponse.getCmdQuery());
            log.info(llmCmdParsedResponse.getCmdQuery());
            //TODO: решить, будут ли доступны многострочные вызовы или нет
            String[] queries = new String[1];
            queries[0] = llmCmdParsedResponse.getCmdQuery();
            System.out.println("Список вызовов: " + queries.length);
            StringBuilder result = new StringBuilder();
            
            try {
//                value = "[CMD вернул ответ]: " +cmdGo(llmCmdParsedResponse.getCmdQuery());
                result.append("[CMD вернул]:");
                Arrays.stream(queries).forEach(x-> {
                    try {
                        result.append(cmdGo(x));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                result.append("[Ошибка при выполнении CMD запроса] StackTrace: \n ").append(sw);
            }
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", result.toString());
            return true;
        }
        return false;
    }

    @Override
    public boolean callWorker(Command command) {
        return false;
    }

    private String cmdGo(String query) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", query);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(),"866"))){
            String line;

            while (true) {
                line = r.readLine();
                if (line == null) {
                    break;
                }
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            throw e;
        }
        return stringBuilder.toString();
    }
}

