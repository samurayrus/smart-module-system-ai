package ru.samurayrus.smartmodulesystemai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import ru.samurayrus.smartmodulesystemai.config.LLMConfig;

@EnableConfigurationProperties
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SmartModuleSystemAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartModuleSystemAiApplication.class, args);
    }

}
