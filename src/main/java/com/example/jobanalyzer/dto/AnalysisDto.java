package com.example.jobanalyzer.dto;


import com.example.jobanalyzer.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public record AnalysisDto (
        Long id,
        String jobDescription,
        String resumeText,
        String resultJson,
        Instant createdAt
){}
