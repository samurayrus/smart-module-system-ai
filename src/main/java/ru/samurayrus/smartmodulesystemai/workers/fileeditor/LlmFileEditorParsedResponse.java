package ru.samurayrus.smartmodulesystemai.workers.fileeditor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmFileEditorParsedResponse {
    private boolean hasFileEditor;
    private String userMessage;
    private FileEditorEnum fileEditorEnum;
    private String filePath;
    private String text;
}
