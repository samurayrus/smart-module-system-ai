package ru.samurayrus.smartmodulesystemai.workers.cmd;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.samurayrus.smartmodulesystemai.gui.ContextStorage;
import ru.samurayrus.smartmodulesystemai.workers.WorkerEventDataBus;
import ru.samurayrus.smartmodulesystemai.workers.WorkerListener;

import java.io.*;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.modules.databaseworker", name = "enabled", havingValue = "true")
public class CmdWorker implements WorkerListener {
    private final WorkerEventDataBus workerEventDataBus;
    private final ContextStorage contextStorage;
    LlmCmdResponseParser llmCmdResponseParser = new LlmCmdResponseParser();

    @Autowired
    public CmdWorker(ContextStorage contextStorage, WorkerEventDataBus workerEventDataBus) {
        this.workerEventDataBus = workerEventDataBus;
        this.contextStorage = contextStorage;
    }

    @PostConstruct
    void registerWorker() {
        log.info("CmdWorker init registration as worker...");
        workerEventDataBus.registerWorker(this);
    }

    @Override
    public boolean callWorker(String content) {
        LlmCmdParsedResponse llmCmdParsedResponse = llmCmdResponseParser.parseResponse(content);

        if (llmCmdParsedResponse.isHasCmd()) {
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", "[Запрос к CMD]: " + llmCmdParsedResponse.getCmdQuery());
            String value;
            try {
                value = "[Ответ успешен! CMD запрос выполнен]: " +cmdGo(llmCmdParsedResponse.getCmdQuery());
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                value = "[Ошибка при выполнении CMD запроса] StackTrace: \n " + sw;
            }
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", value);
            return true;
        }
        return false;
    }

    private String cmdGo(String query) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", query);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
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

