package ru.samurayrus.smartmodulesystemai.workers.fileeditor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmFileEditorParsedResponse {
    private boolean hasFileEditor;
    private String userMessage;
    private FileEditorEnum fileEditorEnum;
    private String filePath;
    private String text;
    private int numStart;
    private int numEnd;
}
