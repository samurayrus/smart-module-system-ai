package ru.samurayrus.smartmodulesystemai.workers.fileeditor;

import java.util.regex.Pattern;

public enum FileEditorEnum {
    CREATE_FILE(Pattern.compile("<CREATE_FILE>\\n(.+?)\\n</CREATE_FILE>", Pattern.DOTALL)),
    SET_TEXT_TO_FILE(Pattern.compile("<SET_TEXT_TO_FILE>\\n(.+?)\\n</SET_TEXT_TO_FILE>", Pattern.DOTALL)),
    PUT_TEXT_TO_FILE(Pattern.compile("<PUT_TEXT_TO_FILE>\\n(.+?)\\n</PUT_TEXT_TO_FILE>", Pattern.DOTALL)),
    CREATE_FOLDER(Pattern.compile("<CREATE_FOLDER>\\n(.+?)\\n</CREATE_FOLDER>", Pattern.DOTALL)),
    READ_FILE(Pattern.compile("<READ_FILE>\\n(.+?)\\n</READ_FILE>", Pattern.DOTALL));

    private final Pattern currentPattern;

    FileEditorEnum(Pattern currentPattern) {
        this.currentPattern = currentPattern;
    }

    public Pattern getCurrentPattern() {
        return currentPattern;
    }
}
