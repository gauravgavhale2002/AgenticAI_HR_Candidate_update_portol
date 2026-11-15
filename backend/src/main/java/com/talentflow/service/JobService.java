package com.talentflow.service;

import com.talentflow.entity.Job;
import com.talentflow.exception.ResourceNotFoundException;
import com.talentflow.repository.JobRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<Job> getAllActiveJobs() {
        return jobRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    public Job createJob(Job job) {
        return jobRepository.save(job);
    }
}
