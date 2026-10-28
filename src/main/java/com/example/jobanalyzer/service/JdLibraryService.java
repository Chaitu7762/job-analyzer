package com.example.jobanalyzer.service;

import com.example.jobanalyzer.entity.JobDescription;
import com.example.jobanalyzer.repository.JobDescriptionRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;


@Service
public class JdLibraryService {

    private final JobDescriptionRepository repository;
    private final VectorStore vectorStore;

    public JdLibraryService(JobDescriptionRepository repository, VectorStore vectorStore) {
        this.repository = repository;
        this.vectorStore = vectorStore;
    }

    public JobDescription save(String text){
        JobDescription jd = new JobDescription();
        jd.setText(text);
        jd.setCreatedAt(Instant.now());
        repository.save(jd);

        Document doc = new Document(text, Map.of("jdId",jd.getId().toString()));
        vectorStore.add(List.of(doc));
        return jd;
    }
}
