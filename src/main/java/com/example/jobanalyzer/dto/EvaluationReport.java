package com.example.jobanalyzer.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvaluationReport {
    private int totalCases;
    private int skillMatches;
    private int levelMatches;
    private double skillAccuracy;     // skillMatches / totalCases
    private double levelAccuracy;     // skillMatches / totalCases
}
