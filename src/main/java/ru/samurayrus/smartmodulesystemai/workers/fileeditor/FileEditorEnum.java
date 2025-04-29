package ru.samurayrus.smartmodulesystemai.workers.fileeditor;

import java.util.regex.Pattern;

public enum FileEditorEnum {
    GET_ALL_FILES_BY_DIR(Pattern.compile("<GET_ALL_FILES_BY_DIR>(.+?)</GET_ALL_FILES_BY_DIR>", Pattern.DOTALL)),
    CREATE_FILE(Pattern.compile("<CREATE_FILE>(.+?)</CREATE_FILE>", Pattern.DOTALL)),
    SET_TEXT_TO_FILE(Pattern.compile("<SET_TEXT_TO_FILE>(.+?)</SET_TEXT_TO_FILE>", Pattern.DOTALL)),
    READ_FILE(Pattern.compile("<READ_FILE>(.+?)</READ_FILE>", Pattern.DOTALL)),
    PUT_TEXT_TO_FILE(Pattern.compile("<PUT_TEXT_TO_FILE>(.+?)</PUT_TEXT_TO_FILE>", Pattern.DOTALL)),
    CREATE_FOLDER(Pattern.compile("<CREATE_FOLDER>(.+?)</CREATE_FOLDER>", Pattern.DOTALL));

    private final Pattern currentPattern;

    FileEditorEnum(Pattern currentPattern) {
        this.currentPattern = currentPattern;
    }

    public Pattern getCurrentPattern() {
        return currentPattern;
    }
}
