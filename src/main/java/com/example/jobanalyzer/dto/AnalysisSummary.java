package com.example.jobanalyzer.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisSummary {
    private Long id;
    private Instant createdAt;
    private String jobDescriptionPreview;
    private String level;
    private String experienceYears;
    private List<String> topSkills;
}
