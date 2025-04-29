package ru.samurayrus.smartmodulesystemai.gui;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import ru.samurayrus.smartmodulesystemai.config.LLMConfig;
import ru.samurayrus.smartmodulesystemai.utils.ChatMessage;
import ru.samurayrus.smartmodulesystemai.utils.ChatRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Component
@Getter
public class ContextStorage {
    private String systemPrompt;
    private ChatRequest currentContext;
    private final LLMConfig llmConfig;

    private final GuiService guiService;
    private int tokens = 0;

    public ContextStorage(LLMConfig llmConfig, @Lazy GuiService guiService) {
        this.llmConfig = llmConfig;
        this.guiService = guiService;
    }

    @PostConstruct
    public void readFile() {
        try (InputStream inputStream = new FileInputStream("SystemPrompt.txt")) {
            byte[] dataAsBytes = FileCopyUtils.copyToByteArray(inputStream);
            String data = new String(dataAsBytes, StandardCharsets.UTF_8);
            systemPrompt = data;
            log.info("SystemPrompt was loaded and have length - " + systemPrompt.length());
            tokens += systemPrompt.length() / 4;
            System.out.println("Текущие токены:" + tokens);
        } catch (IOException e) {
            log.error("Error load system prompt from file", e);
            systemPrompt = reservedSystemPrompt;
            log.info("Load reserved system prompt");
        }

        log.info("Loaded model: " + llmConfig.getModel());
        currentContext = makeFirstChatRequest();
    }

    public void addMessageToContextAndMessagesListIfEnabled(final String user, final String content) {
        if (user.equals("tool") || user.equals("assistant") || user.equals("user") || user.equals("system")) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRole(user);
            chatMessage.setContent(content);
            currentContext.getMessages().add(chatMessage);
            tokens += content.length() / 4;
            System.out.println("Текущие токены:" + tokens);
        }

        if (guiService.isGuiIsEnabled())
            guiService.addMessageToPane(user, content);
    }

    public void addReplacerSpecialTagFromWorkerToGuiMessages(Map<String, String> replacers) {
        log.info("Replacer Special Tag {} was registered", replacers.keySet());
        guiService.getReplacerWorkersTags().putAll(replacers);
    }

    private ChatRequest makeFirstChatRequest() {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMaxTokens(3500);
        chatRequest.setFrequencyPenalty(0);
        chatRequest.setModel(llmConfig.getModel());
//        chatRequest.setModel("gemma-3-4b-it-8q");
        chatRequest.setTemperature(1);
        chatRequest.setStream(false);
        chatRequest.setTopP(1);
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(getSystemPrompt());

        ChatMessage userFirstMessage = new ChatMessage();
        userFirstMessage.setRole("user");
        userFirstMessage.setContent("[Start a new chat]");
        chatRequest.setMessages(new ArrayList<>());
        chatRequest.getMessages().add(systemMessage);
        chatRequest.getMessages().add(userFirstMessage);
        return chatRequest;
    }

    private static final String reservedSystemPrompt = """
            Write ИИ-Ассистент's next reply in a fictional chat between ИИ-Ассистент and SamurayRus.
                            
            Ты — интеллектуальный SQL-ассистент для PostgreSQL. Ты должен строго следовать этим правилам:
                            
            1. Режимы работы:
            - Обычный диалог: для повседневного общения и пояснений
            - SQL-режим: когда требуется работа с БД
                            
            2. Правила SQL-режима:
            - Все SQL-запросы должны быть заключены между строгими маркерами:
            <SQL_START>
            [твой запрос]
            <SQL_END>
            - В одном сообщении может быть только ОДИН SQL-блок
            - Между маркерами должен быть только чистый SQL-код
            - Всё что вне SQL-блока считается комментариями для пользователя. По возможности, минимизируй комментарии
                            
            - Если для решения задачи тебе нужно использовать несколько SQL запросов, то напиши сначала один, получи ответ что он выполнен,
            а затем выолни следующий запрос и ДОЖДИСЬ ответа об успешности.
            - Если в процессе выполнения ты получишь ошибку, то разберись почему она возникла и попробуй её решить.
            - Если решить не получилось, то обратись к пользователю с анализом проблемы.
            - НЕ ПИШИ БОЛЬШЕ ОДНОГО SQL ЗАПРОСА В ОДНОМ СООБЩЕНИИ!!!
            - Если ты получишь ошибку о отсутствии колонки в таблице, то проверь структуру таблицы. Возможно, она называется иначе.
            3. Примеры корректных запросов:
            Пользователь: "Покажи количество записей"
            Ты:
            <SQL_START>
            SELECT COUNT(*) FROM notes;
            <SQL_END>
                            
            Пользователь: "Создай новую таблицу"
            Ты:
            Сейчас создам таблицу notes
                            
            <SQL_START>
            CREATE TABLE notes (
                id SERIAL PRIMARY KEY,
                content TEXT
            );
            <SQL_END>
                            
            4. Запрещено:
            - Отправлять несколько SQL-блоков в одном сообщении
            - Добавлять комментарии внутри SQL-блока
            - Разрывать SQL-блок на несколько сообщений
            - Использовать SQL-блоки без необходимости
            - Использовать SQL-блоки если ты предоставляешь информацию для справки без цели выполнения в бд
                            
            5. Логика работы:
            - Сначала можешь дать пояснение (необязательно)
            - Затем строгий SQL-блок
            - После получения результата можешь:
              * Дать пояснение пользователю
              * Отправить следующий SQL-блок
              * Запросить уточнения
                            
            6. Для сложных операций:
            - Сначала структурные изменения (DDL)
            - Дождись подтверждения выполнения
            - Затем операции с данными (DML)
            - Всегда проверяй SQL на корректность перед отправкой!
                            
            Пример идеального взаимодействия:
            User: Настрой таблицу для хранения заметок и добавь пример
            AI: Создаю таблицу notes...
                            
            <SQL_START>
            CREATE TABLE notes (
                id SERIAL PRIMARY KEY,
                title VARCHAR(100),
                content TEXT
            );
            <SQL_END>
                            
            [Backend выполняет]
            AI: Добавляю пример заметки
                            
            <SQL_START>
            INSERT INTO notes (title, content) VALUES ('Пример', 'Это тестовая заметка');
            <SQL_END>
                            
            [Backend выполняет]
            AI: Готово! Таблица notes создана и содержит тестовую запись.
            """;
}
