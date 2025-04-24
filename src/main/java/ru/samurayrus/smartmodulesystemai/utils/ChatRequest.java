package ru.samurayrus.smartmodulesystemai.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ChatRequest {
    private List<ChatMessage> messages;
    private String model;
    private double temperature;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private boolean stream;

    @JsonProperty("presence_penalty")
    private double presencePenalty;

    @JsonProperty("frequency_penalty")
    private double frequencyPenalty;

    @JsonProperty("top_p")
    private double topP;
}