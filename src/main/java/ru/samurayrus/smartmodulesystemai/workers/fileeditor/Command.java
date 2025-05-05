package ru.samurayrus.smartmodulesystemai.workers.fileeditor;

public class Command {
    private String name;
    private Arguments arguments;

    // Геттеры и сеттеры
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Arguments getArguments() {
        return arguments;
    }

    public void setArguments(Arguments arguments) {
        this.arguments = arguments;
    }
}