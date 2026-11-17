package com.talentflow.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResumeScreeningAgent {

    private static final Set<String> RESUME_SIGNALS = Set.of(
            "professional experience", "work experience", "employment history", "career summary",
            "professional summary", "technical skills", "core skills", "education", "certifications",
            "projects", "work history", "career objective", "resume", "curriculum vitae");
    private static final Set<String> OFFER_SIGNALS = Set.of(
            "offer of employment", "employment offer", "letter of offer", "offer letter",
            "we are pleased to offer", "we are delighted to offer", "joining date",
            "annual compensation", "terms of employment", "accept this offer");
    private static final List<Pattern> PROFANITY_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(?:fuck(?:ing|ed|er|ers)?|shit(?:ty|ting)?|asshole|bitch(?:es)?|bastard)\\b"));
    private static final List<Pattern> EXPLICIT_CONTENT_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(?:porn(?:ography|ographic)?|xxx|nudes?)\\b"),
            Pattern.compile("(?i)\\b(?:explicit sexual content|sex video|nude photo)\\b"));
    private static final List<String> SKILL_TERMS = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Ruby", "PHP",
            "Spring Boot", "Spring", "React", "Angular", "Vue", "Node.js", "Express",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Oracle", "Hadoop", "Spark",
            "AWS", "Azure", "GCP", "Docker", "Kubernetes", "Terraform", "Jenkins", "Git",
            "HTML", "CSS", "Tailwind", "REST", "GraphQL", "Microservices",
            "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch", "NLP", "LLM",
            "Data Analysis", "Power BI", "Tableau", "Excel", "Figma", "Agile", "Scrum",
            "Selenium", "Playwright", "JUnit", "Mockito", "Linux", "Cybersecurity",
            "Project Management", "Communication", "Leadership", "Salesforce", "SAP");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)\\s+(?:of\\s+)?(?:professional\\s+)?experience\\b");
    private static final Pattern PDF_SIGNATURE = Pattern.compile("^%PDF-");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public ScreeningResult analyze(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please attach your resume");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume must be 10 MB or smaller");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file", exception);
        }
        String header = new String(bytes, 0, Math.min(bytes.length, 5), java.nio.charset.StandardCharsets.US_ASCII);
        if (!PDF_SIGNATURE.matcher(header).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a real PDF resume, not an offer letter or another document");
        }

        String text;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded PDF could not be read", exception);
        }
        if (text.length() < 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This PDF has too little extractable text. Upload a text-based CV (scanned-image PDFs are not supported yet).");
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        long offerSignalCount = OFFER_SIGNALS.stream().filter(normalized::contains).count();
        if (offerSignalCount > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This document appears to be an offer/employment letter, not a CV. Upload your resume instead.");
        }

        long resumeSignalCount = RESUME_SIGNALS.stream().filter(normalized::contains).count();
        if (resumeSignalCount < 2) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "We could not verify this as a CV. Include work experience, education, skills, or projects and upload again.");
        }

        List<String> extractedSkills = extractSkills(normalized);
        Integer extractedExperience = extractExperienceYears(text);
        int confidence = Math.min(98, 60 + (int) resumeSignalCount * 8 + (extractedSkills.isEmpty() ? 0 : 10));
        String summary = "CV indicators found: " + resumeSignalCount
                + "; skills detected: " + extractedSkills.size()
                + (extractedExperience == null ? "." : "; stated experience: " + extractedExperience + " years.");
        List<String> reviewFlags = new ArrayList<>(detectContentIssues(text));
        if (extractedSkills.isEmpty() || extractedExperience == null) {
            List<String> missingDetails = new ArrayList<>();
            if (extractedSkills.isEmpty()) {
                missingDetails.add("supported skills");
            }
            if (extractedExperience == null) {
                missingDetails.add("clearly stated years of experience");
            }
            reviewFlags.add("Resume may be incomplete: " + String.join(" and ", missingDetails)
                    + " could not be detected. Please review manually.");
        }
        return new ScreeningResult(text, extractedSkills, extractedExperience, confidence, summary, reviewFlags);
    }

    public List<String> detectContentIssues(String... content) {
        String combined = String.join(" ", content).toLowerCase(Locale.ROOT);
        List<String> issues = new ArrayList<>();
        if (PROFANITY_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(combined).find())) {
            issues.add("Potential abusive or profane language detected; review manually.");
        }
        if (EXPLICIT_CONTENT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(combined).find())) {
            issues.add("Potential sexually explicit content detected; review manually.");
        }
        return List.copyOf(issues);
    }

    List<String> extractSkills(String normalizedText) {
        List<String> found = new ArrayList<>();
        for (String skill : SKILL_TERMS) {
            String normalizedSkill = skill.toLowerCase(Locale.ROOT);
            Pattern pattern = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(normalizedSkill) + "(?![a-z0-9])");
            if (pattern.matcher(normalizedText).find()) {
                found.add(skill);
            }
        }
        return List.copyOf(found);
    }

    Integer extractExperienceYears(String text) {
        Matcher matcher = EXPERIENCE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    public record ScreeningResult(
            String extractedText,
            List<String> extractedSkills,
            Integer experienceYears,
            int confidence,
            String summary,
            List<String> reviewFlags
    ) {
    }
}
