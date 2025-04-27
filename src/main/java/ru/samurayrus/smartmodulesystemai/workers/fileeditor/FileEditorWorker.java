package ru.samurayrus.smartmodulesystemai.workers.fileeditor;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.samurayrus.smartmodulesystemai.gui.ContextStorage;
import ru.samurayrus.smartmodulesystemai.workers.WorkerEventDataBus;
import ru.samurayrus.smartmodulesystemai.workers.WorkerListener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.modules.databaseworker", name = "enabled", havingValue = "true")
public class FileEditorWorker implements WorkerListener {
    private final WorkerEventDataBus workerEventDataBus;
    private final ContextStorage contextStorage;
    LlmFileEditorResponseParser llmFileEditorResponseParser = new LlmFileEditorResponseParser();

    @Autowired
    public FileEditorWorker(ContextStorage contextStorage, WorkerEventDataBus workerEventDataBus) {
        this.workerEventDataBus = workerEventDataBus;
        this.contextStorage = contextStorage;
    }

    @PostConstruct
    void registerWorker() {
        log.info("File_WORKER init registration as worker...");
        workerEventDataBus.registerWorker(this);
//        ChatMessage sysPromNew = new ChatMessage();
//        sysPromNew.setRole("system");
//        sysPromNew.setContent(
//                """
//                        ТЕБЕ ДОСТУПНЫ ФУНКЦИИ ЧТЕНИЯ/ИЗМЕНЕНИЯ/СОЗДАНИЯ ФАЙЛОВ.
//                        ЧТОБЫ ПРОЧИТАТЬ ФАЙЛ ТЫ ДОЛЖНА ИСПОЛЬЗОВАТЬ СТРУКТУРУ:
//                        <READ_FILE>
//                        <FILE_PATH> ТУТ ТОЛЬКО ПОЛНЫЙ ПУТЬ ДО ФАЙЛА  например D:\\ProjectEditor/test.txt </FILE_PATH>
//                        </READ_FILE>
//                        ЧТОБЫ СОЗДАТЬ ФАЙЛ ТЫ ДОЛЖНА ИСПОЛЬЗОВАТЬ:
//                        <CREATE_FILE>
//                        <FILE_PATH> ТУТ ТОЛЬКО ПОЛНЫЙ ПУТЬ ДО ФАЙЛА  например D:\\ProjectEditor/test.txt </FILE_PATH>
//                        </CREATE_FILE>
//                        ЧТОБЫ ЗАПИСАТЬ ИЛИ ПЕРЕЗАПИСАТЬ ФАЙЛ ТЫ ДОЛЖНА ИСПОЛЬЗОВАТЬ:
//                        <SET_TEXT_TO_FILE>
//                        <FILE_PATH>  ТУТ ТОЛЬКО ПОЛНЫЙ ПУТЬ ДО ФАЙЛА например D:\\ProjectEditor/test.txt </FILE_PATH>
//                        <TEXT>ТУТ ТЕКСТ КОТОРЫЙ ХОЧЕШЬ ЗАПИСАТЬ В ФАЙЛ</TEXT>
//                        </SET_TEXT_TO_FILE>
//
//                        ВНУТРИ ТЭГОВ НЕ ПИШИ СВОИ КОММЕНТАРИИ
//                        ИСПОЛЬЗУЙ ТОЛЬКО ОДНО ДЕЙСТВИЕ ЗА ОДНО СООБЩЕНИЕ
//                        ТЫ МОЖЕШЬ ЛИБО СОЗДАТЬ ФАЙЛ, ЛИБО ПРОЧИТАТЬ, ЛИБО ЗАПИСАТЬ ЗА ОДНО СООБЩЕНИЕ
//                        """
//        );
    }

    @Override
    public boolean callWorker(String content) {
        LlmFileEditorParsedResponse response = llmFileEditorResponseParser.parseResponse(content);

        if (response.isHasFileEditor()) {
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", "[Запрос к FILE_EDITOR. Режим - " + response.getFileEditorEnum().name() + "]: " + response.getFilePath() + " " + response.getText());
            log.info("FILE_EDITOR -" + response.getFileEditorEnum() + ": \n" + response.getFilePath() + " " + response.getText());
            String value = "[FILE_EDITOR вернул]:[EMPTY]";
            try {
                switch (response.getFileEditorEnum()) {
                    case READ_FILE -> value = "[FILE_EDITOR вернул ответ]: " + getTextFromFile(response.getFilePath());
                    case CREATE_FILE -> value = "[FILE_EDITOR вернул ответ]: " + createFile(response.getFilePath());
//                    case CREATE_FOLDER -> value = "[CMD вернул ответ]: " +addTextToFile(llmFileEditorParsedResponse.getFileEditorQuery());
//                    case PUT_TEXT_TO_FILE -> value = "[CMD вернул ответ]: " +putTextToFile(llmFileEditorParsedResponse.getFileEditorQuery());
                    case SET_TEXT_TO_FILE -> value = "[FILE_EDITOR вернул ответ]: " + addTextToFile(response.getFilePath(), response.getText());
                }

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                value = ("[Ошибка при выполнении FILE_EDITOR запроса] StackTrace: \n " + sw);
            }
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", value.toString());
            return true;
        }
        return false;
    }

    public String getTextFromFile(String filePath) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            int numRow = 0;
            while (line != null) {
                sb.append(numRow).append(": ").append(line);
                sb.append(System.lineSeparator());
                numRow++;
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    public String putTextToFile(String filePath, int numBegin, int numEnd, String text) {
        return "";
    }

    public String createFile(String filePath) throws IOException {
        Files.write(Paths.get(filePath), Collections.singleton(" "), StandardCharsets.UTF_8);
        return "[Файл " + filePath + " создан!]";
    }

    public String addTextToFile(String filePath, String text) throws IOException {
        Files.write(Paths.get(filePath), Collections.singleton(text), StandardCharsets.UTF_8);
        return "[В файл " + filePath + " добавлен текст!]";
    }
}

