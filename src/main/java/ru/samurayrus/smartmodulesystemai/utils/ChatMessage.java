package ru.samurayrus.smartmodulesystemai.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessage {
    private String role;
    /**
     * В мультимодальности - великолепное поле. (сарказм, ждем пр)
     */
    private String content;
}
