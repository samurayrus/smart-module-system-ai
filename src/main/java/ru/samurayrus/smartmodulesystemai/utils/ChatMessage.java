package ru.samurayrus.smartmodulesystemai.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessage {
    private String role;
    /**
     * В мультимодальности - великолепное поле. Тут находится либо String, либо массив из Image+Text
     */
    private Object content;
}
