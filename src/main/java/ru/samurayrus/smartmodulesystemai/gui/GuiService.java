package ru.samurayrus.smartmodulesystemai.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.samurayrus.smartmodulesystemai.proxy.AiWorkerService;
import ru.samurayrus.smartmodulesystemai.proxy.ChatMessage;
import ru.samurayrus.smartmodulesystemai.proxy.ChatRequest;
import ru.samurayrus.smartmodulesystemai.proxy.GptController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

@Service
public class GuiService {

    private final DefaultListModel<String> messageListModel = new DefaultListModel<>();
    @Getter
    private ChatRequest currentContext;
    public final GptController gptController;
    private final AiWorkerService aiWorkerService;

    @Autowired
    public GuiService(@Lazy GptController gptController, AiWorkerService aiWorkerService) {
        this.gptController = gptController;
        this.aiWorkerService = aiWorkerService;
        currentContext = makeFirstChatRequest();
    }

    @PostConstruct
    public void createGui() {

        System.setProperty("java.awt.headless", "false");
        // Создаем главное окно
        JFrame frame = new JFrame("Чат на Swing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        // Панель для размещения компонентов
        JPanel panel = new JPanel(new BorderLayout());

        // Список сообщений (верхняя часть)
        JList<String> messageList = new JList<>(messageListModel);
        JScrollPane scrollPane = new JScrollPane(messageList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Панель ввода (нижняя часть)
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("Отправить");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);

        // Обработчик нажатия кнопки
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText().trim();
                if (!message.isEmpty()) {
                    addMessageToContextAndMessagesList("user", message);
                    aiWorkerService.workForAi(GuiService.this);

                    inputField.setText("");
                }
            }
        });

        // Добавляем панель в окно
        frame.add(panel);
        frame.setVisible(true);
    }

    public void addMessageToContextAndMessagesList(final String user, final String content) {
        messageListModel.addElement(user + ": " + content);
        if (user.equals("tool") || user.equals("assistant") || user.equals("user") || user.equals("system")) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRole(user);
            chatMessage.setContent(content);
            currentContext.getMessages().add(chatMessage);
        }
    }

    private ChatRequest makeFirstChatRequest() {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMaxTokens(300);
        chatRequest.setFrequencyPenalty(0);
        chatRequest.setModel("gemma-3-12b-it-qat");
        chatRequest.setTemperature(1);
        chatRequest.setStream(false);
        chatRequest.setTopP(1);
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent("Write ИИ-Ассистент's next reply in a fictional chat between ИИ-Ассистент and SamurayRus.\n\nТы — интеллектуальный SQL-ассистент для PostgreSQL. Ты должен строго следовать этим правилам:\n\n1. Режимы работы:\n- Обычный диалог: для повседневного общения\n- SQL-режим: ТОЛЬКО когда требуется работа с БД\n\n2. Правила SQL-режима:\n- Каждый SQL-запрос отправляется ОТДЕЛЬНЫМ сообщением\n- Запрос начинается СТРОГО с символа `!` без других символов перед ним\n- Никаких пояснений до/после запроса\n- Многострочные запросы оформляются БЕЗ `!` на каждой строке\n- После получения результата можешь отправить следующий запрос\n\n3. Примеры корректных запросов:\nПользователь: \"Создай таблицу notes\"\nТы: !CREATE TABLE notes (id SERIAL PRIMARY KEY, text_data TEXT)\n\n[После выполнения]\nПользователь: \"Добавь тестовую запись\"\nТы: !INSERT INTO notes (text_data) VALUES ('Тестовая запись')\n\n4. Запрещено:\n- Добавлять \"SQL-режим\" или другие комментарии к запросам\n- Отправлять несколько запросов в одном сообщении\n- Разрывать один SQL-запрос на несколько сообщений\n\n5. Для DDL+DML операций:\n- Сначала отправь запрос на создание/изменение структуры\n- Дождись подтверждения выполнения\n- Только потом отправляй запросы на заполнение данных\n\n6. Формат многострочных запросов (правильно):\n!CREATE TABLE example (\n    id SERIAL PRIMARY KEY,\n    data JSONB\n)\n\n7. После получения результатов можешь:\n- Сформулировать ответ на естественном языке\n- Отправить следующий SQL-запрос если нужно\n- Запросить уточнения у пользователя\n\nВажно: Всегда проверяй SQL на корректность перед отправкой!\nПример идеального взаимодействия:\n\nUser: Создай таблицу notes и добавь тестовую запись\nAI: !CREATE TABLE notes (id SERIAL PRIMARY KEY, text_data TEXT)\n[Backend выполняет и возвращает результат]\nAI: !INSERT INTO notes (text_data) VALUES ('Это таблица для хранения контекста бесед')\n[Backend выполняет и возвращает результат]\nAI: Готово! Таблица notes создана и содержит тестовую запись.\n\n[Start a new Chat]");
        ChatMessage userFirstMessage = new ChatMessage();
        userFirstMessage.setRole("user");
        userFirstMessage.setContent("[Start a new chat]");
        chatRequest.setMessages(new ArrayList<>());
        chatRequest.getMessages().add(systemMessage);
        chatRequest.getMessages().add(userFirstMessage);
        return chatRequest;
    }
}
