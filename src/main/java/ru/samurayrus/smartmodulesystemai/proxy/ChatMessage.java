package ru.samurayrus.smartmodulesystemai.proxy;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessage {
    private String role;
    private String content;
}
