package ru.samurayrus.smartmodulesystemai.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.samurayrus.smartmodulesystemai.config.LLMConfig;
import ru.samurayrus.smartmodulesystemai.gui.GuiService;
import ru.samurayrus.smartmodulesystemai.utils.ChatRequest;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GlobalWorkerService {
    private GuiService guiService;
    private final ObjectMapper mapper;
    private final WorkerEventDataBus workerEventDataBus;
    private final LLMConfig llmConfig;
    private final RestTemplate restTemplate;
    private final HttpHeaders headers;

    @Autowired
    public GlobalWorkerService(WorkerEventDataBus workerEventDataBus, LLMConfig llmConfig) {
        this.workerEventDataBus = workerEventDataBus;
        this.llmConfig = llmConfig;
        mapper = new ObjectMapper();

        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (llmConfig.isNeedAuth())
            headers.setBearerAuth(llmConfig.getApiKey());
    }

    public void workForAi(GuiService guiService) {
        if (this.guiService == null)
            this.guiService = guiService;

        log.info("---GlobalWorkerService <--> llm --- \n ");

        try {
            boolean isComplete = false;
            while (!isComplete) {
                //Подсасываем актуальный контекст беседы
                String fullPrompt = mapper.writeValueAsString(guiService.getCurrentContext());
                log.info("---Промпт для работы: \n " + fullPrompt + "\n ---");


                ChatRequest chatRequest = mapper.readValue(fullPrompt, ChatRequest.class);

                HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(chatRequest), headers);
                // Выполнение запроса к LLM API
                ResponseEntity<String> response = restTemplate.exchange(llmConfig.getUrl() + "/chat/completions", HttpMethod.POST, request, String.class);

                // Проверка статуса ответа
                if (response.getStatusCode().is2xxSuccessful()) {

                    //  Получаем content, а т.е ответ от нейронки
                    String content = getContentFromJsonFromAi(response.getBody());
                    guiService.addMessageToPane("assistant", content);

                    // Отправляем сообщение от нейронки активным воркерам и либо отправляем результат нейронке, либо отстанавливаем работу
                    isComplete = !callWorkersIfNeed(content);
                    // Отправляем нейронке результат и ждем реакции при повторе
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

    private boolean callWorkersIfNeed(String contentFromLLM) {
        return workerEventDataBus.callActivityWorkers(contentFromLLM);
    }
}
