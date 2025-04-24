package ru.samurayrus.smartmodulesystemai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SmartModuleSystemAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartModuleSystemAiApplication.class, args);
    }

}
