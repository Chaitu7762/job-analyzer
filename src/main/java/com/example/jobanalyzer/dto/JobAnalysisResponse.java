package com.example.jobanalyzer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobAnalysisResponse {
    private List<String> skills;
    private String level;
    private String experienceYears;
    private String summary;
    // NEW: JDs retrieved by RAG that influenced this analysis
    private List<ReferencedJd> referencedJds;

    // NEW: Nested DTO for each referenced JD
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferencedJd {
        private Long jdId;              // id from job_descriptions table
        private String textPreview;     // first ~100 chars for display
        private Double score;           // similarity score (optional, may be null)
    }
}
