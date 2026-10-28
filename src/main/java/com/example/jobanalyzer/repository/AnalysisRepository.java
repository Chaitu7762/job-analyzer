package com.example.jobanalyzer.repository;

import com.example.jobanalyzer.entity.Analysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis,Long> {
    Page<Analysis> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
