package com.example.jobanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {
    private String model;
    private String prompt;
    private boolean stream;
    private String format;                              // "json" for structured output
    @Builder.Default
    private String keep_alive = "24h";                  // keep model warm across calls
}