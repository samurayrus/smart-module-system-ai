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
import java.util.*;

/**
 * Выполняет операции над файлами
 * Создание, редактирование, чтение с номерами строк, получение списка файлов в каталоге и подкаталогах
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.modules.file-editor-worker", name = "enabled", havingValue = "true")
public class FileEditorWorker implements WorkerListener {
    private final LlmFileEditorResponseParser llmFileEditorResponseParser = new LlmFileEditorResponseParser();
    private final WorkerEventDataBus workerEventDataBus;
    private final ContextStorage contextStorage;

    @Autowired
    public FileEditorWorker(ContextStorage contextStorage, WorkerEventDataBus workerEventDataBus) {
        this.workerEventDataBus = workerEventDataBus;
        this.contextStorage = contextStorage;
    }

    @PostConstruct
    void registerWorker() {
        log.info("File_WORKER init registration as worker...");
        workerEventDataBus.registerWorker(this);

        Map<String, String> tagsMag = new HashMap<>();
        tagsMag.put("<GET_ALL_FILES_BY_DIR>", "<span style='color:green;'>&lt;GET_ALL_FILES_BY_DIR&gt;");
        tagsMag.put("<SET_TEXT_TO_FILE>", "&lt;SET_TEXT_TO_FILE&gt;</span>");
        tagsMag.put("<READ_FILE>", "<span style='color:green;'>&lt;READ_FILE&gt;");
        tagsMag.put("<PUT_TEXT_TO_FILE>", "&lt;PUT_TEXT_TO_FILE&gt;</span>");
        tagsMag.put("</GET_ALL_FILES_BY_DIR>", "<span style='color:green;'>&lt;/GET_ALL_FILES_BY_DIR&gt;");
        tagsMag.put("</SET_TEXT_TO_FILE>", "&lt;/SET_TEXT_TO_FILE&gt;</span>");
        tagsMag.put("</READ_FILE>", "<span style='color:green;'>&lt;/READ_FILE&gt;");
        tagsMag.put("</PUT_TEXT_TO_FILE>", "&lt;/PUT_TEXT_TO_FILE&gt;</span>");
        contextStorage.addReplacerSpecialTagFromWorkerToGuiMessages(tagsMag);
    }

    @Override
    public boolean callWorker(String content) {
        LlmFileEditorParsedResponse response = llmFileEditorResponseParser.parseResponse(content);

        if (response.isHasFileEditor()) {
            response.setText(response.getText().replaceAll("\\n", "\n"));
            contextStorage.addMessageToContextAndMessagesListIfEnabled(
                    "tool",
                    "[Запрос к FILE_EDITOR. Режим - %s]: Path: [%s] Text: [%s]".formatted(response.getFileEditorEnum().name(), response.getFilePath(), response.getText())
            );

            log.info("FILE_EDITOR. Режим - {}]: \nPath: [{}] \nText: [{}]", response.getFileEditorEnum().name(), response.getFilePath(), response.getText());

            String result = doFileEditionAndReturnResult(response);

            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", result);
            return true;
        }
        return false;
    }

    private String doFileEditionAndReturnResult(LlmFileEditorParsedResponse response) {
        String result = "[FILE_EDITOR вернул]:[EMPTY]";
        try {
            switch (response.getFileEditorEnum()) {
                case READ_FILE -> result = "[FILE_EDITOR вернул ответ]: " + getTextFromFile(response.getFilePath());
                case CREATE_FILE -> result = "[FILE_EDITOR вернул ответ]: " + createFile(response.getFilePath(), response.getText());
//                    case CREATE_FOLDER -> value = "[CMD вернул ответ]: " +addTextToFile(llmFileEditorParsedResponse.getFileEditorQuery());
                case PUT_TEXT_TO_FILE -> result = "[FILE_EDITOR вернул ответ]: " + putTextToFile(response.getFilePath(), response.getNumStart(), response.getNumEnd(), response.getText());
                case SET_TEXT_TO_FILE -> result = "[FILE_EDITOR вернул ответ]: " + addTextToFile(response.getFilePath(), response.getText());
                case GET_ALL_FILES_BY_DIR -> result = "[FILE_EDITOR вернул ответ]: " + getAllFilesFromDirectory(response.getFilePath());
            }

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result = ("[Ошибка при выполнении FILE_EDITOR запроса] StackTrace: \n " + sw);
        }
        return result;
    }

    private String getTextFromFile(String filePath) throws Exception {
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

    private String getAllFilesFromDirectory(String path) {
        List<String> filePaths = new ArrayList<>();
        File root = new File(path);

        if (!root.exists()) {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }

        collectFiles(root, filePaths);
        return String.join("\n", filePaths);
    }

    private void collectFiles(File file, List<String> filePaths) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    collectFiles(f, filePaths);
                }
            }
        } else {
            filePaths.add(file.getAbsolutePath());
        }
    }

    private String putTextToFile(String filePath, int numBegin, int numEnd, String text) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            if (numBegin < 0 || numBegin > numEnd) {
                return "[Ошибка: Некорректные номера строк!]";
            }

            List<String> updatedLines = new ArrayList<>();
            boolean addedText = false;
            for (int i = 0; i < lines.size(); i++) {
                if (i >= numBegin && i <= numEnd) {
                    if (addedText) continue;
                    addedText = true;
                    updatedLines.add(text);
                } else {
                    updatedLines.add(lines.get(i));
                }
            }
            //Если модель выходит за рамки файла, то добавляем в конец
            if (numEnd >= lines.size() && !addedText)
                updatedLines.add(text);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                for (String updatedLine : updatedLines) {
                    bw.write(updatedLine);
                    bw.newLine(); // Добавляем новую строку после каждой строки
                }
            }

            return "[Текст успешно заменен в файле %s!] [Обновленный файл: %s] ".formatted(filePath, getTextFromFile(filePath));
        } catch (Exception e) {
            return "[Ошибка: Некорректные номера строк!]";
        }
    }

    @Deprecated
    private String createFile(String filePath, String text) throws IOException {
        Files.write(Paths.get(filePath), Collections.singleton(text != null ? text : " "), StandardCharsets.UTF_8);
        return "[Файл " + filePath + " создан!]";
    }

    private String addTextToFile(String filePath, String text) throws IOException {
        Files.write(Paths.get(filePath), Collections.singleton(text), StandardCharsets.UTF_8);
        return "[В файл " + filePath + " добавлен текст!]";
    }
}

