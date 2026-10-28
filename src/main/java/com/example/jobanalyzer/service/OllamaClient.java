package com.example.jobanalyzer.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.example.jobanalyzer.dto.OllamaRequest;
import com.example.jobanalyzer.dto.OllamaResponse;

@Service
public class OllamaClient {
    
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    public OllamaClient(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    public String generateJobJson(String prompt) {
        OllamaRequest request = OllamaRequest.builder()
                .model("llama3")
                .prompt(prompt)
                .stream(false)
                .format("json")             // ← structured output
                // .keep_alive("24h")       // default is already 24h via @Builder.Default
                .build();

        String url = "http://localhost:11434/api/generate";
        OllamaResponse response = restTemplate.postForObject(url, request, OllamaResponse.class);

        if (response == null || response.getResponse() == null) {
            throw new IllegalStateException("Empty response from Ollama");
        }

        log.debug("Ollama raw output: {}", response.getResponse());
        return response.getResponse();
    }
    
}
