package ru.samurayrus.smartmodulesystemai.utils;

import org.springframework.boot.configurationprocessor.json.JSONObject;

public class Command {
    private String name;
    private JSONObject arguments;

    // Геттеры и сеттеры
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JSONObject getArguments() {
        return arguments;
    }

    public void setArguments(JSONObject arguments) {
        this.arguments = arguments;
    }
}