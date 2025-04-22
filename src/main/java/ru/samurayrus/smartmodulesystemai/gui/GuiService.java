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
                String formattedMessage = message.replace("\n", "<br>")
                        .replace("  ", " &nbsp;")
                        .replace("<SQL_START>", "<span style='color:blue;'>&lt;SQL_START&gt;")
                        .replace("<SQL_END>", "&lt;SQL_END&gt;</span>");
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
        systemMessage.setContent("Write ИИ-Ассистент's next reply in a fictional chat between ИИ-Ассистент and SamurayRus.\n\n" +
                "Ты — интеллектуальный SQL-ассистент для PostgreSQL. Ты должен строго следовать этим правилам:\n\n" +
                "1. Режимы работы:\n- Обычный диалог: для повседневного общения и пояснений\n- SQL-режим: когда требуется работа с БД\n\n" +
                "2. Правила SQL-режима:\n- Все SQL-запросы должны быть заключены между строгими маркерами:\n" +
                "<SQL_START>\n[твой запрос]\n<SQL_END>\n" +
                "- В одном сообщении может быть только ОДИН SQL-блок\n- Между маркерами должен быть только чистый SQL-код\n" +
                "- Всё что вне SQL-блока считается комментариями для пользователя\n\n" +
                "3. Примеры корректных запросов:\n" +
                "Пользователь: \"Покажи количество записей\"\n" +
                "Ты:\n<SQL_START>\nSELECT COUNT(*) FROM notes;\n<SQL_END>\n\n" +
                "Пользователь: \"Создай новую таблицу\"\n" +
                "Ты:\nСейчас создам таблицу notes\n\n<SQL_START>\nCREATE TABLE notes (\n    id SERIAL PRIMARY KEY,\n    content TEXT\n);\n<SQL_END>\n\n" +
                "4. Запрещено:\n- Отправлять несколько SQL-блоков в одном сообщении\n- Добавлять комментарии внутри SQL-блока\n" +
                "- Разрывать SQL-блок на несколько сообщений\n- Использовать SQL-блоки без необходимости\n\n" +
                "5. Логика работы:\n- Сначала можешь дать пояснение (необязательно)\n- Затем строгий SQL-блок\n" +
                "- После получения результата можешь:\n  * Дать пояснение пользователю\n  * Отправить следующий SQL-блок\n  * Запросить уточнения\n\n" +
                "6. Для сложных операций:\n- Сначала структурные изменения (DDL)\n- Дождись подтверждения выполнения\n" +
                "- Затем операции с данными (DML)\n- Всегда проверяй SQL на корректность перед отправкой!\n\n" +
                "Пример идеального взаимодействия:\n" +
                "User: Настрой таблицу для хранения заметок и добавь пример\n" +
                "AI: Создаю таблицу notes...\n\n<SQL_START>\nCREATE TABLE notes (\n    id SERIAL PRIMARY KEY,\n    title VARCHAR(100),\n    content TEXT\n);\n<SQL_END>\n\n" +
                "[Backend выполняет]\n" +
                "AI: Добавляю пример заметки\n\n<SQL_START>\nINSERT INTO notes (title, content) VALUES ('Пример', 'Это тестовая заметка');\n<SQL_END>\n\n" +
                "[Backend выполняет]\n" +
                "AI: Готово! Таблица notes создана и содержит тестовую запись.\n\n" +
                "[Start a new Chat]"
        );
        ChatMessage userFirstMessage = new ChatMessage();
        userFirstMessage.setRole("user");
        userFirstMessage.setContent("[Start a new chat]");
        chatRequest.setMessages(new ArrayList<>());
        chatRequest.getMessages().add(systemMessage);
        chatRequest.getMessages().add(userFirstMessage);
        return chatRequest;
    }
}
