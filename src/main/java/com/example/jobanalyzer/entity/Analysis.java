package com.example.jobanalyzer.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
/*
 It is created to store each analysis result so users can revisit it.
 */
@Entity
@Data
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    // Instant is a precise timestamp for machine time
    private Instant createdAt;

}
