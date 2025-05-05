package ru.samurayrus.smartmodulesystemai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "app.llm")
@Getter
@Setter
public class LLMConfig {
    private String model;
    private String url;
    private String apiKey;
    private boolean needAuth;
    //llm params
    private double temperature;
    private int maxTokens;
    private double presencePenalty;
    private double frequencyPenalty;
    private double topP;
}
