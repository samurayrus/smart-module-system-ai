package ru.samurayrus.smartmodulesystemai.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@Configuration
@ConfigurationProperties(prefix = "app.llm")
@Getter
@Setter
public class LLMConfig {
    private String model;
    private String url;
    private String apiKey;
    private boolean needAuth;
}
