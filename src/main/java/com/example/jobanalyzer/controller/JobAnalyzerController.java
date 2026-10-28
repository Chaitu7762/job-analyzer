package com.example.jobanalyzer.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.example.jobanalyzer.dto.AnalysisSummary;
import com.example.jobanalyzer.dto.EvaluationReport;
import com.example.jobanalyzer.entity.JobDescription;
import com.example.jobanalyzer.service.EvaluationService;
import com.example.jobanalyzer.service.JdLibraryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.jobanalyzer.dto.JobAnalysisResponse;
import com.example.jobanalyzer.service.JobAnalysisService;

@RestController
@RequestMapping("/jobs")
public class JobAnalyzerController {

    private final JobAnalysisService jobAnalysisService;
    private final JdLibraryService jdLibraryService;
    private final EvaluationService evaluationService;

    public JobAnalyzerController(JobAnalysisService jobAnalysisService, JdLibraryService jdLibraryService, EvaluationService evaluationService) {
        this.jobAnalysisService = jobAnalysisService;
        this.jdLibraryService = jdLibraryService;
        this.evaluationService = evaluationService;
    }

    /**
      @AuthenticationPrincipal is a Spring Security annotation used to inject the currently logged-in user's information directly into a controller method parameter.
    */
    @PostMapping("/analyze")
    public JobAnalysisResponse analyzeJobDescription(@RequestParam("resume") MultipartFile resume,
                                                     @RequestParam("jobDescription") String jobDescription, @AuthenticationPrincipal String username) throws IOException {
        return jobAnalysisService.analyzeJobDescription(resume, jobDescription, username);
    }

    /**
     * Returns all past job analyses of the currently logged-in user,
     * newest first. Used so users can revisit their previous results
     * without re-running the (slow/expensive) Ollama analysis.
     */
    @GetMapping("/analyses")
    public Page<AnalysisSummary> getMyAnalyses(@AuthenticationPrincipal String username,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return jobAnalysisService.getMyAnalyses(username, PageRequest.of(page, size));
    }

    @PostMapping("/library")
    public JobDescription addToLibrary(@RequestBody Map<String, String> body) {
        return jdLibraryService.save(body.get("text"));
    }

    @GetMapping("/admin/eval")
    public EvaluationReport runEvaluation() throws IOException {
        return evaluationService.runEvaluation();
    }

}
