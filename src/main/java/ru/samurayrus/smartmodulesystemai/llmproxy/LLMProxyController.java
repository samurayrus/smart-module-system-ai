package ru.samurayrus.smartmodulesystemai.llmproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.samurayrus.smartmodulesystemai.config.LLMConfig;
import ru.samurayrus.smartmodulesystemai.utils.ChatRequest;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "v1")
public class LLMProxyController {
    private final LLMConfig llmConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders headers = new HttpHeaders();

    @Autowired
    public LLMProxyController(LLMConfig llmConfig) {
        this.llmConfig = llmConfig;
        headers.setContentType(MediaType.APPLICATION_JSON);

        //TODO: посмотреть на поведение и если что заменить на headers.set("Authorization", "Bearer " + apiToken);
        if (llmConfig.isNeedAuth())
            headers.setBearerAuth(llmConfig.getApiKey());
    }

    @GetMapping("/models")
    public ResponseEntity<String> getModels() {
        //TODO: сделать получение и выбор модели
        String mes = " { \"data\": [ { \"id\": \"gemma-3-4b-it-8q\", \"object\": \"model\", \"owned_by\": \"organization_owner\"}], \"object\": \"list\"}";
        return new ResponseEntity<>(mes, HttpStatus.OK);
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<String> getChatCompletions(@RequestBody String prompt) {
        return new ResponseEntity<>(proxyRequest(prompt.replaceFirst("\"stream\":true", "\"stream\":false"), "/chat/completions"), HttpStatus.OK);
    }

    @Deprecated
    @PostMapping("/completions")
    public ResponseEntity<String> getCompletions(@RequestBody String prompt) {
        return new ResponseEntity<>(proxyRequest(prompt, "/completions"), HttpStatus.OK);
    }

    //TODO: переделать метод. Поведение не соответствует
    @PostMapping("/embeddings")
    public ResponseEntity<String> getEmb(@RequestBody String prompt) {
        return new ResponseEntity<>(proxyRequest(prompt, "/embeddings"), HttpStatus.OK);
    }

    public String proxyRequest(String prompt, String endpoint) {
        log.info("URL: [{}], Prompt: [{}]", llmConfig.getUrl() + endpoint, prompt);

        try {
            ChatRequest chatRequest = new ObjectMapper().readValue(prompt, ChatRequest.class);

            HttpEntity<String> request = new HttpEntity<>(new ObjectMapper().writeValueAsString(chatRequest), headers);
            // Выполнение запроса к LLM API
            ResponseEntity<String> response = restTemplate.exchange(llmConfig.getUrl() + endpoint, HttpMethod.POST, request, String.class);

            // Проверка статуса ответа
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new Exception("LLM API returned an error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error during LLM API call ", e); // Логирование ошибок
            return "Error 500";
        }
    }
}
