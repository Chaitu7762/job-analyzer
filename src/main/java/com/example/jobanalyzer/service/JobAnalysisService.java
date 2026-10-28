package com.example.jobanalyzer.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.jobanalyzer.dto.AnalysisDto;
import com.example.jobanalyzer.dto.AnalysisSummary;
import com.example.jobanalyzer.entity.Analysis;
import com.example.jobanalyzer.entity.User;
import com.example.jobanalyzer.repository.AnalysisRepository;
import com.example.jobanalyzer.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.jobanalyzer.dto.JobAnalysisResponse;

@Service
public class JobAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(JobAnalysisService.class);
    private final ResumeParserService resumeParserService;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final VectorStore vectorStore;

    public JobAnalysisService(OllamaClient ollamaClient, ObjectMapper objectMapper,
            ResumeParserService resumeParserService, UserRepository userRepository,
            AnalysisRepository analysisRepository, VectorStore vectorStore) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
        this.resumeParserService = resumeParserService;
        this.userRepository = userRepository;
        this.analysisRepository = analysisRepository;
        this.vectorStore = vectorStore;
    }

    public JobAnalysisResponse analyzeJobDescription(MultipartFile resume, String jobDescription, String username)
            throws IOException {
        String resumeText = resumeParserService.extractText(resume);

        // 1. Retrieve similar JDs
        List<Document> similarJds = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(resumeText)
                        .topK(3)
                        .build());

        String res = ollamaClient.generateJobJson(createPromptforOllama(resumeText, jobDescription, similarJds));
        String extractedJson = extractJson(res);
        System.out.println(extractedJson);
        JobAnalysisResponse response = objectMapper.readValue(extractedJson, JobAnalysisResponse.class);
        // NEW: attach the retrieved JDs to the response (before saving + returning)
        response.setReferencedJds(mapToReferencedJds(similarJds));
        addResponseToAnalysisTable(jobDescription, resumeText, extractedJson, username);
        return response;
    }

    private void addResponseToAnalysisTable(String jobDescription, String resumeText, String response, String username)
            throws JsonProcessingException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        Analysis analysis = new Analysis();
        analysis.setUser(user);
        analysis.setJobDescription(jobDescription);
        analysis.setResumeText(resumeText);
        analysis.setResultJson(response);
        analysis.setCreatedAt(Instant.now());
        analysisRepository.save(analysis);
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start == -1 || end == -1 || start > end) {
            throw new IllegalArgumentException("Invalid JSON given by ollama");
        }
        return response.substring(start, end + 1);
    }

    private String createPromptforOllama(String resumeText, String jobDescription, List<Document> similarJds) {
        String referenceContext = similarJds.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        return """
                You are a job-fit analyzer. Your FIRST task is to compute the candidate's level using ONLY their years of professional experience from the resume.

                === STEP 1: COMPUTE LEVEL (DO THIS FIRST) ===
                Count the candidate's years of professional full-time experience from the RESUME.
                Then apply this formula:
                - 0-1 years  → Intern
                - 1-3 years  → Junior
                - 3-6 years  → Mid
                - 6-10 years → Senior
                - 10+ years  → Lead
                
                EXAMPLES:
                - B.Tech 2023-2027, no work experience → level = "Intern", experienceYears = "0 years"
                - 2 years as Software Engineer → level = "Junior", experienceYears = "2 years"
                - 4 years as Software Engineer → level = "Mid", experienceYears = "4 years"
                - "3+ years" stated in resume summary → level = "Mid", experienceYears = "3+ years"

                IMPORTANT: Do NOT use job titles (e.g., "Senior Software Engineer") as evidence of seniority.
                Do NOT use the JD's title as evidence of the candidate's level.
                Do NOT use the reference JDs' titles. Only years of experience count.

                If the resume says "3+ years", the level is "Mid" — regardless of any title.

                === STEP 2: EXTRACT SKILLS ===
                Extract skills ONLY from the RESUME. Each item MUST be one skill (no categories, no combined strings).
                Do NOT include parenthetical descriptions.
                Do NOT extract skills from the job description or reference JDs.

                === STEP 3: OUTPUT ===
                Return ONLY valid JSON with this schema. Every field is REQUIRED:
                {
                  "level": "Intern" | "Junior" | "Mid" | "Senior" | "Lead",
                  "experienceYears": "X years",
                  "skills": ["skill1", "skill2"],
                  "summary": "1-2 sentence summary"
                }

                === REFERENCE JDs (for summary context only — do not use for level or skills) ===
                %s

                === RESUME (only source for skills and level) ===
                %s

                === JOB DESCRIPTION (target role — DO NOT use its title for level) ===
                %s
                """
                .formatted(
                        referenceContext.isBlank() ? "(none)" : referenceContext,
                        resumeText,
                        jobDescription);
    }

    public Page<AnalysisSummary> getMyAnalyses(String username, Pageable pageable) {
        return analysisRepository
                .findByUserUsernameOrderByCreatedAtDesc(username, pageable)
                .map(this::toSummary);
    }

    private AnalysisSummary toSummary(Analysis a) {
        String preview = a.getJobDescription() == null
                ? ""
                : a.getJobDescription().trim().substring(
                        0, Math.min(100, a.getJobDescription().trim().length()));

        String level = null;
        String experienceYears = null;
        List<String> topSkills = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(a.getResultJson());
            level = root.path("level").asText(null);
            experienceYears = root.path("experienceYears").asText(null);
            JsonNode skillsNode = root.path("skills");
            if (skillsNode.isArray()) {
                for (int i = 0; i < Math.min(3, skillsNode.size()); i++) {
                    topSkills.add(skillsNode.get(i).asText());
                }
            }
        } catch (Exception e) {
            // corrupted JSON — return preview only
        }

        return new AnalysisSummary(
                a.getId(),
                a.getCreatedAt(),
                preview,
                level,
                experienceYears,
                topSkills);
    }

    private List<JobAnalysisResponse.ReferencedJd> mapToReferencedJds(List<Document> similarJds) {
        return similarJds.stream()
                .map(doc -> new JobAnalysisResponse.ReferencedJd(
                        parseJdId(doc.getMetadata().get("jdId")),
                        buildPreview(doc.getText()),
                        null // score — populate later if you extract similarity scores
                ))
                .toList();
    }

    private Long parseJdId(Object raw) {
        if (raw == null)
            return null;
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildPreview(String text) {
        if (text == null)
            return "";
        String trimmed = text.trim();
        return trimmed.length() <= 100 ? trimmed : trimmed.substring(0, 100) + "…";
    }

    public JobAnalysisResponse analyzeText(String resumeText, String jobDescription, String username)
            throws IOException {
        List<Document> similarJds = vectorStore.similaritySearch(
                SearchRequest.builder().query(resumeText).topK(3).build());
        String res = ollamaClient.generateJobJson(
                createPromptforOllama(resumeText, jobDescription, similarJds));
        String extractedJson = extractJson(res);
        log.info("RAW LLM JSON: {}", extractedJson);
        JobAnalysisResponse response = objectMapper.readValue(extractedJson, JobAnalysisResponse.class);
        log.info("PARSED level={}, experienceYears={}, skillsCount={}",
                response.getLevel(),
                response.getExperienceYears(),
                response.getSkills() == null ? 0 : response.getSkills().size());
        response.setReferencedJds(mapToReferencedJds(similarJds));
        // No DB save during evaluation — cleaner
        return response;
    }
}
