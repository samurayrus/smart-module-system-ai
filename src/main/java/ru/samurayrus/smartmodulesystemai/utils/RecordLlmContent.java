package ru.samurayrus.smartmodulesystemai.utils;

import java.util.Map;

public record RecordLlmContent(String content, Map<String, String> toolFunction) {
    //[{id=675888457, type=function, function={name=search_files_for_path, arguments={"path":"D:\\ProjectEditor"}}}]
}
