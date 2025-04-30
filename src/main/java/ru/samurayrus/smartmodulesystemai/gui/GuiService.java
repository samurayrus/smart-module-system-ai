package ru.samurayrus.smartmodulesystemai.gui;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.samurayrus.smartmodulesystemai.workers.GlobalWorkerService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GuiService {
    private final GlobalWorkerService globalWorkerService;
    private JTextPane pane;
    private final ContextStorage contextStorage;
    @Value("${app.modules.gui.enabled}")
    @Getter
    private boolean isGuiIsEnabled;
    @Getter
    private final Map<String, String> replacerWorkersTags = new HashMap<>();

    @Autowired
    public GuiService(GlobalWorkerService globalWorkerService, ContextStorage contextStorage) {
        this.globalWorkerService = globalWorkerService;
        this.contextStorage = contextStorage;

        replacerWorkersTags.put("\n", "<br>");
//        replacerWorkersTags.put("  ", "&nbsp;");
    }

    @PostConstruct
    public void createGui() {
        log.info("GUIService is starting...");
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

        //Панель ввода
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

        //Регистрируем вставку картинок через CTRL + V
        registerPasteOperationForImageAndText(inputField);
        //Регистрируем для DragAndDrop картинки
        registerDragAndDropEventForImages(inputField);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        ActionListener sendAction = e -> sendMessage(inputField);

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        // Делаем окно видимым после всей настройки
        frame.setVisible(true);
    }

    void addMessageToPane(String sender, String message) {
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

                String formattedMessage = message;
                // Замена служебных тэгов на отображаемые (от воркеров и тп)
                for (String replace : replacerWorkersTags.keySet()) {
                    formattedMessage = formattedMessage.replace(replace, replacerWorkersTags.get(replace));
                }

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
                    ((Timer) e.getSource()).stop();
                    animateNewMessage(doc, startPos);
                });
                timer.setRepeats(false);
                timer.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMessage(JTextField inputField) {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            contextStorage.addMessageToContextAndMessagesListIfEnabled("user", message);
            inputField.setText("");

            new Thread(globalWorkerService::workForAi).start();
        }
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
                    if (count >= blinkCount * 2) {
                        // Восстанавливаем оригинальный фон
                        setElementBackground(doc, messageElem, new Color(0, 0, 0, 0));
                        blinkTimer.stop();
                        return;
                    }

                    try {
                        Color bgColor = (count % 2 == 0) ?
                                new Color(220, 220, 255) : // цвет мигания
                                new Color(0, 0, 0, 0);        // прозрачный

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

    private void registerPasteOperationForImageAndText(JTextField inputField) {
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    try {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        Transferable content = clipboard.getContents(null);

                        if (content == null || !clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor))
                            return;

                        sendImageToAiWithAddToGuiMessage((BufferedImage) content.getTransferData(DataFlavor.imageFlavor));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void registerDragAndDropEventForImages(JTextField inputField) {
        inputField.setDragEnabled(true);
        new DropTarget(inputField, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    //Сделал забор только первого элемента из drag and drop файлов, т.к больше одного вроде не поддерживается все равно.
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        java.util.List<File> fileList = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        byte[] bytes = Files.readAllBytes(fileList.get(0).toPath());
                        addMessageToPane("user", "[ОТПРАВЛЕНО ИЗОБРАЖЕНИЕ. РАЗМЕР: " + bytes.length + "]");
                        contextStorage.sendMessageWithImage(bytes);
                        globalWorkerService.workForAi();
                    }
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    dtde.dropComplete(false);
                }
            }
        });
    }

    private void sendImageToAiWithAddToGuiMessage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        addMessageToPane("user", "[ОТПРАВЛЕНО ИЗОБРАЖЕНИЕ. РАЗМЕР: " + bytes.length + "]");
        contextStorage.sendMessageWithImage(bytes);
        globalWorkerService.workForAi();
    }
}
