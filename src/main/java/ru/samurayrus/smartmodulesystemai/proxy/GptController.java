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

    @Autowired
    public GptController() {
    }

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

        return new ResponseEntity<>(send(prompt.replaceFirst("\"stream\":true", "\"stream\":false"), "http://localhost:1234/v1/chat/completions"), HttpStatus.OK);
    }

    @PostMapping("/completions")
    public ResponseEntity<String> getCompletions(@RequestBody String prompt) {
        System.out.println("/completions");
        return new ResponseEntity<>(send(prompt, "http://localhost:1234/v1/completions"), HttpStatus.OK);
    }

    @PostMapping("/embeddings")
    public ResponseEntity<String> getEmb(@RequestBody String prompt) {
        System.out.println("/embeddings");
        return new ResponseEntity<>(send(prompt, "http://localhost:1234/v1/embeddings"), HttpStatus.OK);
    }

    public String send(String prompt, String endpoint) {

        System.out.println(endpoint + " \n prompt: \n" + prompt);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Добавление заголовков, если необходимо (например, API токен)
        //request.getHeaders().set("Authorization", "Bearer " + apiToken); // Пример авторизации

        try {
            ChatRequest chatRequest;
            chatRequest = new ObjectMapper().readValue(prompt, ChatRequest.class);


            HttpEntity<String> request = new HttpEntity<>(new ObjectMapper().writeValueAsString(chatRequest), headers);
            // Выполнение запроса к LLM API
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

            // Проверка статуса ответа
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new Exception("LLM API returned an error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error during LLM API call: " + e.getMessage()); // Логирование ошибок
            return "error 212";
        }
    }
}
