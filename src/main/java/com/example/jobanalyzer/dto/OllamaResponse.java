package com.example.jobanalyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OllamaResponse {
    private String response;
    public OllamaResponse(String response){
        this.response = response;
    }
}
