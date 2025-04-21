package ru.samurayrus.smartmodulesystemai.proxy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.samurayrus.smartmodulesystemai.gui.GuiService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "v1")
public class GptController {
    JdbcTemplate jdbcTemplate;
    GuiService guiService;

    @Autowired
    public GptController(JdbcTemplate jdbcTemplate, GuiService guiService) {
        this.jdbcTemplate = jdbcTemplate;
        this.guiService = guiService;
    }

    //    public GptController() throws JsonProcessingException {
//        String promptTest = "{\"messages\":[{\"role\":\"system\",\"content\":\"Write ИИ-Ассистент's next reply in a fictional chat between ИИ-Ассистент and SamurayRus.\\n\\nИИ-Помощник. Старается сделать все качественно, быстро и не запутаться в длинном диалоге. Пишешь только на русском. Отвечаешь коротко и ясно.\\n\\n[Start a new Chat]\"},{\"role\":\"user\",\"content\":\"[Start a new chat]\"},{\"role\":\"assistant\",\"content\":\"Привет! Или как говорит мой пользователь, Бомжур :)\"},{\"role\":\"user\",\"content\":\"Привет! напиши слово пчела\"}],\"model\":\"gemma-3-4b-it-8q\",\"temperature\":1,\"max_tokens\":300,\"stream\":false,\"presence_penalty\":0,\"frequency_penalty\":0,\"top_p\":1} ";
//        ChatRequest chatRequest = new ObjectMapper().readValue(promptTest, ChatRequest.class);
//        System.out.println(chatRequest.getMessages().size());
//    }

    @GetMapping("/models")
    public ResponseEntity<String> getModels() {
//        String mes = " { \"data\": [ { \"id\": \"gemma-3-4b-it-8q\", \"object\": \"model\", \"owned_by\": \"organization_owner\"}], \"object\": \"list\"}";
        String mes = " { \"data\": [ { \"id\": \"gemma-3-12b-it-qat\", \"object\": \"model\", \"owned_by\": \"organization_owner\"}], \"object\": \"list\"}";
        return new ResponseEntity<>(mes, HttpStatus.OK);
    }


    /*
    example  body

    {"model": "gemma-3-4b-it-8q", "messages": [
    {"role": "system", "content": "Сегодня 20.04.2025"},
     {"role": "user", "content": "Как сделать так, чтобы ты запоминал предыдущие запросы?"}
     ],
      "temperature": 0.7, "max_tokens": -1, "stream": false
}
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<String> getChatCompletions(@RequestBody String prompt) {
        System.out.println("/chat/completions");

        return new ResponseEntity<>(send(prompt.replaceFirst("\"stream\":true", "\"stream\":false"), "http://localhost:1234/v1/chat/completions", null), HttpStatus.OK);
    }

    @PostMapping("/completions")
    public ResponseEntity<String> getCompletions(@RequestBody String prompt) {
        System.out.println("/completions");
        return new ResponseEntity<>(send(prompt, "http://localhost:1234/v1/completions", null), HttpStatus.OK);
    }

    @PostMapping("/embeddings")
    public ResponseEntity<String> getEmb(@RequestBody String prompt) {
        System.out.println("/embeddings");
        return new ResponseEntity<>(send(prompt, "http://localhost:1234/v1/embeddings", null), HttpStatus.OK);
    }

    public String send(String prompt, String endpoint, ChatRequest chatRequesta) {

        System.out.println(prompt + " \n" + endpoint);
//        ChatRequest chatRequest = new ObjectMapper().convertValue(prompt, ChatRequest.class);
//        System.out.println(chatRequest.getMessages().size());
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Создание HTTP-запроса
//        String promptTest = "{\"messages\":[{\"role\":\"system\",\"content\":\"Write ИИ-Ассистент's next reply in a fictional chat between ИИ-Ассистент and SamurayRus.\\n\\nИИ-Помощник. Старается сделать все качественно, быстро и не запутаться в длинном диалоге. Пишешь только на русском. Отвечаешь коротко и ясно.\\n\\n[Start a new Chat]\"},{\"role\":\"user\",\"content\":\"[Start a new chat]\"},{\"role\":\"assistant\",\"content\":\"Привет! Или как говорит мой пользователь, Бомжур :)\"},{\"role\":\"user\",\"content\":\"Привет! напиши слово пчела\"}],\"model\":\"gemma-3-4b-it-8q\",\"temperature\":1,\"max_tokens\":300,\"stream\":false,\"presence_penalty\":0,\"frequency_penalty\":0,\"top_p\":1} ";


        // Добавление заголовков, если необходимо (например, API токен)
//        request.getHeaders().set("Authorization", "Bearer " + apiToken); // Пример авторизации

        try {
            ChatRequest chatRequest;
            if (chatRequesta == null) {
                guiService.addMessageToPane("back", prompt + endpoint);
                chatRequest = new ObjectMapper().readValue(prompt, ChatRequest.class);
            }
            else{
                guiService.addMessageToPane("back-cyl", prompt + endpoint);
                chatRequest = chatRequesta;
            }
//        System.out.println(chatRequest.getMessages().size());

            HttpEntity<String> request = new HttpEntity<>(new ObjectMapper().writeValueAsString(chatRequest), headers);
            // Выполнение запроса к LLM API
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

            // Проверка статуса ответа
            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                String respString = response.getBody();

                // 1. Парсим основной ответ
                Map<String, Object> body = mapper.readValue(respString, new TypeReference<Map<String, Object>>() {
                });

                // 2. Достаем список choices и первый элемент
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                Map<String, Object> firstChoice = choices.get(choices.size()-1);

                // 3. Достаем message как Map (без преобразования в строку и обратно)
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

                // 4. Получаем content
                String content = (String) message.get("content");
                guiService.addMessageToPane("back-content", content);
                System.out.println("Content: " + content);

                if (content.startsWith("!")) {
                    System.out.println("\n ---- \n Запрос к бд" + content + " \n ---");
                    guiService.addMessageToPane("tool", "[Запрос к бд]: " + content);
                    String value = "Ответ нет";
                    try {
                        value = "[Ответ успешен! Sql запрос выполнен]" + processSqlResult(executeSql(content.replaceFirst("!", " ")));
                    }catch (Exception e){
                        value= "[Ошибка при выполнении запроса]" + e.getMessage();
                    }
                    guiService.addMessageToPane("tool", value);

                    ChatMessage cm = new ChatMessage();
                    cm.setRole("tool");
                    cm.setContent("Получен ответ. Напиши в последнем сообщении  резуаьтат обработки ответа, чтобы не потерять контекст, иначе после ответа пользователю ты забудешь о запросах. Ответ - " + value);
                    System.out.println("\n --- Result: \n"+cm.getContent() + " \n ---\n");
                    chatRequest.getMessages().add(cm);

                    return send(prompt, endpoint, chatRequest);
                }
                return respString;
            } else {
                throw new Exception("LLM API returned an error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error during LLM API call: " + e.getMessage()); // Логирование ошибок
            return "error 212";
        }
    }

    public String processSqlResult(Object sqlResult) {
        if (sqlResult == null) {
            return "Нет данных";
        }

        if (sqlResult instanceof List) {
            return formatListResult((List<?>) sqlResult);
        } else if (sqlResult instanceof Map) {
            return formatMapResult((Map<?, ?>) sqlResult);
        } else if (sqlResult instanceof Integer || sqlResult instanceof Long) {
            return "Операция успешна! Теперь можно отвечать пользователю или выполнять другие команды, которые необходимы для выполнения текущей задачи. Затронуто строк: " + sqlResult;
        } else {
            return sqlResult.toString();
        }
    }

    public Object executeSql(String sql) {
        sql = sql.trim().toLowerCase();

        try {
            if (sql.startsWith("select")) {
                // Для запросов с возвратом данных
                return jdbcTemplate.queryForList(sql);
            } else if (sql.startsWith("insert") || sql.startsWith("update")
                    || sql.startsWith("delete") || sql.startsWith("merge")) {
                // Для изменяющих запросов
                return jdbcTemplate.update(sql);
            } else if (sql.startsWith("create") || sql.startsWith("alter")
                    || sql.startsWith("drop") || sql.startsWith("truncate")) {
                // Для DDL-запросов
                jdbcTemplate.execute(sql);
                return "DDL операция выполнена успешно";
            } else {
                // Для других случаев (хранимые процедуры и т.д.)
                return jdbcTemplate.queryForList(sql);
            }
        } catch (DataAccessException e) {
            throw new RuntimeException("Ошибка выполнения SQL: " + e.getMessage(), e);
        }
    }

    private String formatListResult(List<?> list) {
        if (list.isEmpty()) {
            return "Нет данных";
        }

        StringBuilder sb = new StringBuilder();
        if (list.get(0) instanceof Map) {
            // Результат с несколькими колонками
            for (Object item : list) {
                Map<?, ?> row = (Map<?, ?>) item;
                sb.append(row.values().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" | ")))
                        .append("\n");
            }
        } else {
            // Результат с одной колонкой
            list.forEach(item -> sb.append(item.toString()).append("\n"));
        }
        return sb.toString();
    }

    private String formatMapResult(Map<?, ?> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

}


/*
List<ChatMessage> messages = List.of(
    new ChatMessage("system", "Write ИИ-Ассистент's next reply..."),
    new ChatMessage("user", "[Start a new chat]"),
    // ... остальные сообщения
);

// Создание запроса
ChatRequest request = new ChatRequest();
request.setMessages(messages);
request.setModel("gemma-3-4b-it-8q");
request.setTemperature(1);
request.setMaxTokens(300);
request.setStream(false);
request.setPresencePenalty(0);
request.setFrequencyPenalty(0);
request.setTopP(1);

// Сериализация в JSON
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(request);
 */
