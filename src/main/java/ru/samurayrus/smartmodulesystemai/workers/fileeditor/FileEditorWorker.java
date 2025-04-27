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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    }

    @Override
    public boolean callWorker(String content) {
        LlmFileEditorParsedResponse response = llmFileEditorResponseParser.parseResponse(content);

        if (response.isHasFileEditor()) {
            response.setText(response.getText().replaceAll("\\n", "\n"));
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", "[Запрос к FILE_EDITOR. Режим - " + response.getFileEditorEnum().name() + "]: " + response.getFilePath() + " " + response.getText());
            log.info("FILE_EDITOR -" + response.getFileEditorEnum() + ": \n" + response.getFilePath() + " " + response.getText());
            String value = "[FILE_EDITOR вернул]:[EMPTY]";
            try {
                switch (response.getFileEditorEnum()) {
                    case READ_FILE -> value = "[FILE_EDITOR вернул ответ]: " + getTextFromFile(response.getFilePath());
                    case CREATE_FILE -> value = "[FILE_EDITOR вернул ответ]: " + createFile(response.getFilePath());
//                    case CREATE_FOLDER -> value = "[CMD вернул ответ]: " +addTextToFile(llmFileEditorParsedResponse.getFileEditorQuery());
                    case PUT_TEXT_TO_FILE -> value = "[FILE_EDITOR вернул ответ]: " + putTextToFile(response.getFilePath(), response.getNumStart(), response.getNumEnd(), response.getText());
                    case SET_TEXT_TO_FILE -> value = "[FILE_EDITOR вернул ответ]: " + addTextToFile(response.getFilePath(), response.getText());
                    case GET_ALL_FILES_BY_DIR -> value = "[FILE_EDITOR вернул ответ]: " + getAllFilesFromDirectory(response.getFilePath());
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

//    @PostConstruct
//    public void test() throws IOException {
//        addTextToFile("D:\\ProjectEditor\\test.txt","Привет! \n Это тест переноса строк! \n Как делак?");
//        putTextToFile("D:\\ProjectEditor\\test.txt",1,1,"Замнил Привет на \n ХАЛЛОУ");
//    }

    public String getAllFilesFromDirectory(String path){
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

    public String putTextToFile(String filePath, int numBegin, int numEnd, String text) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            if (numBegin < 0 || numEnd >= lines.size() || numBegin > numEnd) {
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

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                for (String updatedLine : updatedLines) {
                    bw.write(updatedLine);
                    bw.newLine(); // Добавляем новую строку после каждой строки
                }
            }

            return "[Текст успешно заменен в файле " + filePath + "!]";

        } catch (NumberFormatException e) {
            return "[Ошибка: Некорректные номера строк (не числа)!]";
        }
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

