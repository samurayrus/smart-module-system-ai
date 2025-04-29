package ru.samurayrus.smartmodulesystemai.workers.fileeditor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LlmFileEditorResponseParser {

    public LlmFileEditorParsedResponse parseResponse(String llmResponse) {
        LlmFileEditorParsedResponse response = new LlmFileEditorParsedResponse();

        for(FileEditorEnum fileEditorEnum : FileEditorEnum.values()) {
            Matcher cmdMatcher = fileEditorEnum.getCurrentPattern().matcher(llmResponse);
            if (cmdMatcher.find()) {
                // Получаем команду
                String cmd = cmdMatcher.group(1);
                response.setHasFileEditor(true);
                response.setFileEditorEnum(fileEditorEnum);
                response.setFilePath(filePathExtractor(cmd));
                response.setText(fileTextExtractor(cmd));
                response.setNumStart(findNumStart(cmd));
                response.setNumEnd(findNumEnd(cmd));
                // Вырезаем командную часть чтобы получить только комментарии
                response.setUserMessage(llmResponse.replace(cmdMatcher.group(0), "").trim());
                break;
            } else {
                response.setHasFileEditor(false);
                response.setUserMessage(llmResponse.trim());
            }
        }

        return response;
    }

    //<FILE_PATH>D:\ProjectEditor\carrots.txt</FILE_PATH>
    Pattern findPathPattern = Pattern.compile("<FILE_PATH>(.*?)</FILE_PATH>", Pattern.DOTALL);
    private String filePathExtractor(String message){
        Matcher cmdMatcher = findPathPattern.matcher(message);
        if(cmdMatcher.find()){
            return cmdMatcher.group(1);
        }
        return "[Путь не задан]";
    }

    Pattern findTextPattern = Pattern.compile("<TEXT>(.+?)</TEXT>", Pattern.DOTALL);
    private String fileTextExtractor(String message){
        Matcher cmdMatcher = findTextPattern.matcher(message);
        if(cmdMatcher.find()){
            return cmdMatcher.group(1);
        }
        return "";
    }

    Pattern findNumStartPattern = Pattern.compile("<NUM_START>(.+?)</NUM_START>", Pattern.DOTALL);
    private int findNumStart(String message){
        Matcher cmdMatcher = findNumStartPattern.matcher(message);
        if(cmdMatcher.find()){
            return Integer.parseInt(cmdMatcher.group(1));
        }
        return -1;
    }

    Pattern findNumEndPattern = Pattern.compile("<NUM_END>(.+?)</NUM_END>", Pattern.DOTALL);
    private int findNumEnd(String message){
        Matcher cmdMatcher = findNumEndPattern.matcher(message);
        if(cmdMatcher.find()){
            return Integer.parseInt(cmdMatcher.group(1));
        }
        return -1;
    }
}
