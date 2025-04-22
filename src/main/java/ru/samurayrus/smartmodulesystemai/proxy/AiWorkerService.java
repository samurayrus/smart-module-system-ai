package ru.samurayrus.smartmodulesystemai.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.samurayrus.smartmodulesystemai.databases.DataBaseWorkerService;
import ru.samurayrus.smartmodulesystemai.databases.LlmResponseParser;
import ru.samurayrus.smartmodulesystemai.databases.ParsedResponse;
import ru.samurayrus.smartmodulesystemai.gui.GuiService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiWorkerService {
    private GuiService guiService;
    private final DataBaseWorkerService dataBaseWorkerService;
    private final ObjectMapper mapper;
    private final LlmResponseParser responseParser = new LlmResponseParser();

    @Autowired
    public AiWorkerService(DataBaseWorkerService dataBaseWorkerService) {
        this.dataBaseWorkerService = dataBaseWorkerService;

        mapper = new ObjectMapper();
    }

    public void workForAi(GuiService guiService) {
        if (this.guiService == null)
            this.guiService = guiService;

        System.out.println("---Запущена работа с ai--- \n ");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            boolean isComplete = false;
            while (!isComplete) {
                //Подсасываем актуальный контекст беседы
                String fullPrompt = mapper.writeValueAsString(guiService.getCurrentContext());
                System.out.println("---Промпт для работы: \n " + fullPrompt + "\n ---");


                ChatRequest chatRequest = mapper.readValue(fullPrompt, ChatRequest.class);
//                guiService.addMessageToPane("worker-ai", fullPrompt);


                HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(chatRequest), headers);
                // Выполнение запроса к LLM API
                ResponseEntity<String> response = restTemplate.exchange("http://192.168.0.105:1234/v1/chat/completions", HttpMethod.POST, request, String.class);

                // Проверка статуса ответа
                if (response.getStatusCode().is2xxSuccessful()) {

                    //  Получаем content, а т.е ответ от нейронки
                    String content = getContentFromJsonFromAi(response.getBody());
                    guiService.addMessageToPane("assistant", content);

                    // Если нейронка написала сообщение с ! то выполняем работы с бд
                    isComplete = !checkForSqlQueryInContentAndWorkWithParser(content);
                    // Отправляем нейронке результат и ждем реакции
                    // <--
                } else {
                    throw new Exception("LLM API returned an error: " + response.getStatusCode());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при работе с llm", e);
            guiService.addMessageToPane("worker-ai", "Ошибка при работе с llm - " + e.getMessage());
        }
    }

    public String getContentFromJsonFromAi(String jsonAiAnswer) throws JsonProcessingException {
        // 1. Парсим основной ответ
        Map<String, Object> body = mapper.readValue(jsonAiAnswer, new TypeReference<Map<String, Object>>() {
        });

        // 2. Достаем список choices и последний элемент
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> firstChoice = choices.get(choices.size() - 1);

        // 3. Достаем message как Map (без преобразования в строку и обратно)
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        return (String) message.get("content");
    }

    public boolean checkForSqlQueryInContentAndWorkWithParser(String content) {
        ParsedResponse parsedResponse = responseParser.parseResponse(content);

        if (parsedResponse.isHasSql()) {
            guiService.addMessageToPane("tool", "[Запрос к бд]: " + parsedResponse.getSqlQuery());
            String value;
            try {
                value = "[Ответ успешен! Sql запрос выполнен]: " + dataBaseWorkerService.executeSql(parsedResponse.getSqlQuery());
            } catch (Exception e) {
                value = "[Ошибка при выполнении запроса]" + e.getMessage();
            }
            guiService.addMessageToPane("tool", value);
            return true;
        }
        return false;
    }

    public boolean checkForSqlQueryInContentAndWork(String content) {
        if (content.startsWith("!")) {
            guiService.addMessageToPane("tool", "[Запрос к бд]: " + content);
            String value;
            try {
                value = "[Ответ успешен! Sql запрос выполнен]: " + dataBaseWorkerService.executeSql(content.replaceFirst("!", " "));
            } catch (Exception e) {
                value = "[Ошибка при выполнении запроса]" + e.getMessage();
            }
            guiService.addMessageToPane("tool", value);
            return true;
        }
        return false;
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
            } else {
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
                Map<String, Object> firstChoice = choices.get(choices.size() - 1);

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
                        value = "[Ответ успешен! Sql запрос выполнен]" + dataBaseWorkerService.executeSql(content.replaceFirst("!", " "));
                    } catch (Exception e) {
                        value = "[Ошибка при выполнении запроса]" + e.getMessage();
                    }
                    guiService.addMessageToPane("tool", value);

                    ChatMessage cm = new ChatMessage();
                    cm.setRole("tool");
                    cm.setContent("Получен ответ. Напиши в последнем сообщении  резуаьтат обработки ответа, чтобы не потерять контекст, иначе после ответа пользователю ты забудешь о запросах. Ответ - " + value);
                    System.out.println("\n --- Result: \n" + cm.getContent() + " \n ---\n");
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


}
