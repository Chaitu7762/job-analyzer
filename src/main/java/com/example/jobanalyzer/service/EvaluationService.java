package com.example.jobanalyzer.service;

import com.example.jobanalyzer.dto.EvaluationReport;
import com.example.jobanalyzer.dto.GoldenCase;
import com.example.jobanalyzer.dto.JobAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final JobAnalysisService jobAnalysisService;
    private final ObjectMapper objectMapper;

    public EvaluationService(JobAnalysisService jobAnalysisService, ObjectMapper objectMapper) {
        this.jobAnalysisService = jobAnalysisService;
        this.objectMapper = objectMapper;
    }

    public EvaluationReport runEvaluation() throws IOException {
        List<GoldenCase> cases = loadGoldenDataset();
        log.info("Loaded {} golden cases", cases.size());

        int total = 0;
        int skillMatches = 0;
        int levelMatches = 0;

        for (GoldenCase gc : cases) {
            try {
                // Analyze with a fixed "evaluator" username (must exist in DB, see Step 5)
                JobAnalysisResponse actual = jobAnalysisService
                        .analyzeText(gc.getResumeText(), gc.getJobDescription(), "evaluator");

                total++;

                // --- Skill match: at least 70% of expected skills are in the output ---
                Set<String> expectedSkills = gc.getExpected().getSkills().stream()
                        .map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> actualSkills = actual.getSkills() == null ? Set.of() :
                        actual.getSkills().stream()
                                .map(String::toLowerCase).collect(Collectors.toSet());

                long matched = expectedSkills.stream().filter(actualSkills::contains).count();
                double coverage = expectedSkills.isEmpty() ? 0 : (double) matched / expectedSkills.size();
                if (coverage >= 0.7) skillMatches++;

                // --- Level match: exact match on level string ---
                if (gc.getExpected().getLevel().equalsIgnoreCase(actual.getLevel())) {
                    levelMatches++;
                }

                log.info("Case {}: skillCoverage={}% level={} (expected {}), actual {}",
                        gc.getId(),
                        String.format("%.0f", coverage * 100),
                        actual.getLevel(),
                        gc.getExpected().getLevel(),
                        actual.getLevel());

            } catch (Exception e) {
                log.warn("Case {} failed: {}", gc.getId(), e.getMessage());
            }
        }

        double skillAccuracy = total == 0 ? 0 : (double) skillMatches / total;
        double levelAccuracy = total == 0 ? 0 : (double) levelMatches / total;

        return new EvaluationReport(total, skillMatches, levelMatches, skillAccuracy, levelAccuracy);
    }

    private List<GoldenCase> loadGoldenDataset() throws IOException {
        ClassPathResource resource = new ClassPathResource("golden-dataset.json");
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<GoldenCase>>() {});
        }
    }
}