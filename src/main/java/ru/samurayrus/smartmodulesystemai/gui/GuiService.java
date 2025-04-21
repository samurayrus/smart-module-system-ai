package ru.samurayrus.smartmodulesystemai.gui;

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
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

@Service
public class GuiService {

    @Getter
    private ChatRequest currentContext;
    public final GptController gptController;
    private final AiWorkerService aiWorkerService;
    private JTextPane pane;

    @Autowired
    public GuiService(@Lazy GptController gptController, AiWorkerService aiWorkerService) {
        this.gptController = gptController;
        this.aiWorkerService = aiWorkerService;
        currentContext = makeFirstChatRequest();
    }

    @PostConstruct
    public void createGui() {
        System.setProperty("java.awt.headless", "false");

        // Создаем главное окно с улучшенным дизайном
        JFrame frame = new JFrame("AI Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 600);
        frame.setLayout(new BorderLayout());

        try {
            // Устанавливаем красивый LookAndFeel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Панель сообщений с возможностью копирования и переноса текста
        JTextPane messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setContentType("text/html");
        messagePane.setEditorKit(new HTMLEditorKit());
        messagePane.setBackground(new Color(240, 240, 240));
        messagePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane = messagePane;

        JScrollPane scrollPane = new JScrollPane(messagePane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Панель ввода с улучшенным дизайном
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(new Color(250, 250, 250));

        JTextField inputField = new JTextField();
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JButton sendButton = new JButton("Отправить");
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Добавляем компоненты в окно
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Обработчики событий
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(inputField);
            }
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        // Делаем окно видимым после всей настройки
        frame.setVisible(true);
    }

    private void sendMessage(JTextField inputField) {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            addMessageToPane("user", message);
            inputField.setText("");

            new Thread(() -> {
                aiWorkerService.workForAi(GuiService.this);
            }).start();
        }
    }

    public void addMessageToPane(String sender, String message) {
        addMessageToContextAndMessagesList(sender, message);
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLEditorKit kit = (HTMLEditorKit) pane.getEditorKit();
                HTMLDocument doc = (HTMLDocument) pane.getDocument();

                // Цвета для разных отправителей
                Color color = switch (sender) {
                    case "user" -> new Color(70, 130, 180);
                    case "assistant" -> new Color(147, 35, 19);
                    case "worker-ai" -> new Color(180, 180, 180);
                    case "tool" -> new Color(136, 175, 53);
                    default -> Color.BLACK;
                };

                String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                String formattedMessage = message.replace("\n", "<br>").replace("  ", " &nbsp;");
                String senderHtml = "<b>" + sender + ":</b> ";

                // Сохраняем позицию перед вставкой
                int startPos = doc.getLength();

                // Вставляем сообщение с уникальным классом
                String messageHtml = "<div class='new-message' style='color:" + hexColor + "; margin: 5px 0;'>" +
                        senderHtml + formattedMessage + "</div>";

                kit.insertHTML(doc, startPos, messageHtml, 0, 0, null);
                pane.setCaretPosition(doc.getLength());

                // Запускаем анимацию через небольшой таймаут
                Timer timer = new Timer(150, e -> {
                    ((Timer)e.getSource()).stop();
                    animateNewMessage(doc, startPos);
                });
                timer.setRepeats(false);
                timer.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void animateNewMessage(HTMLDocument doc, int startPos) {
        try {
            // Находим элемент по стартовой позиции
            Element elem = doc.getCharacterElement(startPos);
            if (elem == null) return;

            // Получаем корневой элемент для сообщения
            while (elem != null && !elem.getName().equals("div")) {
                elem = elem.getParentElement();
            }
            if (elem == null) return;

            final Element messageElem = elem;
            Color originalBg = pane.getBackground();

            Timer blinkTimer = new Timer(150, null);
            blinkTimer.addActionListener(new ActionListener() {
                int count = 0;
                final int blinkCount = 4;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (count >= blinkCount*2) {
                        // Восстанавливаем оригинальный фон
                        setElementBackground(doc, messageElem, new Color(0,0,0,0));
                        blinkTimer.stop();
                        return;
                    }

                    try {
                        Color bgColor = (count % 2 == 0) ?
                                new Color(220, 220, 255) : // цвет мигания
                                new Color(0,0,0,0);        // прозрачный

                        setElementBackground(doc, messageElem, bgColor);
                        count++;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        blinkTimer.stop();
                    }
                }
            });
            blinkTimer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setElementBackground(HTMLDocument doc, Element elem, Color color) {
        try {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setBackground(attrs, color);
            doc.setCharacterAttributes(
                    elem.getStartOffset(),
                    elem.getEndOffset() - elem.getStartOffset(),
                    attrs,
                    false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMessageToContextAndMessagesList(final String user, final String content) {
        if (user.equals("tool") || user.equals("assistant") || user.equals("user") || user.equals("system")) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRole(user);
            chatMessage.setContent(content);
            currentContext.getMessages().add(chatMessage);
        }
    }

    private ChatRequest makeFirstChatRequest() {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMaxTokens(1500);
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
