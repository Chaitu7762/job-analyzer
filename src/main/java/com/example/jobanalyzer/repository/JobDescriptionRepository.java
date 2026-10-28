package com.example.jobanalyzer.repository;

import com.example.jobanalyzer.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
}
