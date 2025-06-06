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
import ru.samurayrus.smartmodulesystemai.gui.ContextStorage;
import ru.samurayrus.smartmodulesystemai.utils.ChatRequest;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * GlobalWorker Отвечает за все взаимодействия с llm.
 * Вызывает другие воркеры через шину и если они скажут, что требуется отправить результат их работы нейронке, то отправляет и так по кругу.
 */
@Slf4j
@Service
public class GlobalWorkerService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders headers = new HttpHeaders();
    private final WorkerEventDataBus workerEventDataBus;
    private final LLMConfig llmConfig;
    private final ContextStorage contextStorage;

    @Autowired
    public GlobalWorkerService(WorkerEventDataBus workerEventDataBus, LLMConfig llmConfig, ContextStorage contextStorage) {
        this.workerEventDataBus = workerEventDataBus;
        this.llmConfig = llmConfig;
        this.contextStorage = contextStorage;

        headers.setContentType(MediaType.APPLICATION_JSON);
        if (llmConfig.isNeedAuth())
            headers.setBearerAuth(llmConfig.getApiKey());
    }

    public void workForAi() {
        try {
            boolean isComplete = false;
            while (!isComplete) {
                //Подсасываем актуальный контекст беседы
                ChatRequest chatRequest = contextStorage.getCurrentContext();
                //Выполняем запрос к llm и получаем обработанный ответ
                String responseContentWithoutThinking = removeThinkingTagFromResponseContent(sendCurrentContextToLLM(chatRequest));
                //Обновляем контекст
                contextStorage.addMessageToContextAndMessagesListIfEnabled("assistant", responseContentWithoutThinking);
                //Вызываем воркеры
                isComplete = !callWorkersIfNeed(responseContentWithoutThinking);
                // Отправляем нейронке результат и ждем реакции при повторе
                // <--
            }
        } catch (Exception e) {
            log.error("Ошибка при работе с llm", e);
            contextStorage.addMessageToContextAndMessagesListIfEnabled("worker-ai", "Ошибка при работе с llm - " + e.getMessage());
        }
    }

    private String sendCurrentContextToLLM(ChatRequest chatRequest) throws Exception {
        HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(chatRequest), headers);
        ResponseEntity<String> response = restTemplate.exchange(
                llmConfig.getUrl() + "/chat/completions",
                HttpMethod.POST,
                request,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return getContentFromJsonFromAi(response.getBody());
        } else {
            throw new Exception("LLM API returned an error: " + response.getStatusCode());
        }
    }

    private String getContentFromJsonFromAi(String jsonAiAnswer) throws JsonProcessingException {
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

    /**
     * Игнорируем размышления, чтобы не переполнять контекст
     * она подумала, чтобы ответить, ответила и больше нам это не надо (qwen как раз рекомендует так делать)
     **/
    private String removeThinkingTagFromResponseContent(String responseContent) {
        return Pattern.compile("<think>(.+?)</think>", Pattern.DOTALL).matcher(responseContent).replaceAll("[thinking...]");
    }
}
