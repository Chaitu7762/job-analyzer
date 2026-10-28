package com.example.jobanalyzer.dto;


import lombok.Data;

import java.util.List;

@Data
public class GoldenCase {
    private Integer id;
    private String resumeText;
    private String jobDescription;
    private Expected expected;

    @Data
    public static class Expected {
        private List<String> skills;
        private String level;
        private String experienceYears;
    }
}
