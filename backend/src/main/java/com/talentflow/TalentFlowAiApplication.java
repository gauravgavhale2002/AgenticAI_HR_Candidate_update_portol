package com.talentflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class TalentFlowAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentFlowAiApplication.class, args);
    }

    @GetMapping("/api/health")
    public String health() {
        return "TalentFlow AI backend is running";
    }
}
