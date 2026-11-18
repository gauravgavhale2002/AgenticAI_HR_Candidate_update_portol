package com.talentflow.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
            "Project Management", "Communication", "Leadership", "Salesforce", "SAP",
            "Mechanical Engineering", "SolidWorks", "CATIA", "AutoCAD", "CNC", "HVAC",
            "Thermodynamics", "Machine Design", "PLC", "Embedded Systems", "Circuit Design",
            "Civil Engineering", "Structural Engineering", "Revit", "Quantity Surveying",
            "Accounting", "Financial Analysis", "Recruitment", "Digital Marketing");
    private static final List<FieldSignal> FIELD_SIGNALS = List.of(
            new FieldSignal("Software / IT", List.of(
                    "software", "java", "python", "javascript", "react", "spring", "database", "cloud", "devops")),
            new FieldSignal("Mechanical engineering", List.of(
                    "mechanical", "solidworks", "catia", "cnc", "hvac", "thermodynamics", "machine design", "manufacturing")),
            new FieldSignal("Electrical / electronics", List.of(
                    "electrical", "electronics", "circuit", "embedded systems", "pcb", "plc", "microcontroller")),
            new FieldSignal("Civil / construction", List.of(
                    "civil", "structural", "revit", "quantity surveying", "construction", "site engineer")),
            new FieldSignal("Data / analytics", List.of(
                    "data analysis", "machine learning", "power bi", "tableau", "statistics", "data science")),
            new FieldSignal("Finance / accounting", List.of(
                    "accounting", "financial analysis", "audit", "taxation", "bookkeeping")),
            new FieldSignal("Human resources", List.of(
                    "human resources", "recruitment", "talent acquisition", "payroll", "employee relations")),
            new FieldSignal("Marketing / sales", List.of(
                    "digital marketing", "marketing", "sales", "seo", "campaign management", "business development")));
    private static final Pattern WORK_SECTION_PATTERN = Pattern.compile(
            "(?im)(?:^|\\n)[ \\t]*(?:professional experience|work experience|employment history|career history|work history)[ \\t]*:?[ \\t]*(?:\\n|$)");
    private static final Pattern NEXT_SECTION_PATTERN = Pattern.compile(
            "(?i)\\b(?:education|academic background|technical skills|core skills|skills|projects|certifications|references)\\b");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "(?i)\\b(?:(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+)?"
                    + "((?:19|20)\\d{2})\\s*(?:-|\\u2013|\\u2014|to|through)\\s*"
                    + "(?:(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+)?"
                    + "((?:19|20)\\d{2}|present|current|now)\\b");
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
        boolean containsImages = false;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            text = new PDFTextStripper().getText(document)
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\R{2,}", "\n")
                    .trim();
            for (PDPage page : document.getPages()) {
                if (hasEmbeddedImage(page.getResources(), new HashSet<>())) {
                    containsImages = true;
                    break;
                }
            }
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded PDF could not be read", exception);
        }
        if (text.length() < 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This PDF has too little extractable text. Upload a text-based CV (scanned-image PDFs are not supported yet).");
        }

        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        long offerSignalCount = OFFER_SIGNALS.stream().filter(normalized::contains).count();
        if (offerSignalCount > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This document appears to be an offer/employment letter, not a CV. Upload your resume instead.");
        }

        long resumeSignalCount = RESUME_SIGNALS.stream().filter(normalized::contains).count();
        if (resumeSignalCount < 2) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This document does not look like a CV or resume. Certificates and other documents are not accepted. "
                            + "Please upload a proper text-based CV/resume as a PDF, including work experience, education, skills, or projects.");
        }

        List<String> extractedSkills = extractSkills(normalized);
        Integer extractedExperience = extractExperienceYears(text);
        String workHistory = extractWorkHistory(text);
        List<WorkPeriod> workPeriods = extractWorkPeriods(workHistory);
        Integer timelineExperience = calculateTimelineExperience(workPeriods);
        List<String> careerInsights = buildCareerInsights(text, workPeriods, extractedSkills, timelineExperience);
        int confidence = Math.min(98, 60 + (int) resumeSignalCount * 8 + (extractedSkills.isEmpty() ? 0 : 10));
        String summary = "CV indicators found: " + resumeSignalCount
                + "; skills detected: " + extractedSkills.size()
                + (extractedExperience == null ? "." : "; stated experience: " + extractedExperience + " years.");
        List<String> reviewFlags = new ArrayList<>(detectContentIssues(text));
        if (containsImages) {
            reviewFlags.add("Resume contains embedded image(s). Image content was not analyzed; HR must inspect the resume manually.");
        }
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
        return new ScreeningResult(
                text, extractedSkills, extractedExperience, timelineExperience,
                confidence, summary, reviewFlags, careerInsights);
    }

    private boolean hasEmbeddedImage(PDResources resources, Set<COSStream> visitedForms) {
        if (resources == null) {
            return false;
        }
        try {
            for (var name : resources.getXObjectNames()) {
                PDXObject object = resources.getXObject(name);
                if (object instanceof PDImageXObject) {
                    return true;
                }
                if (object instanceof PDFormXObject form
                        && visitedForms.add(form.getCOSObject())
                        && hasEmbeddedImage(form.getResources(), visitedForms)) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect PDF image resources", exception);
        }
    }

    private String extractWorkHistory(String text) {
        Matcher startMatcher = WORK_SECTION_PATTERN.matcher(text);
        if (!startMatcher.find()) {
            return "";
        }
        int start = startMatcher.end();
        Matcher nextMatcher = NEXT_SECTION_PATTERN.matcher(text);
        if (nextMatcher.find(start)) {
            return text.substring(start, nextMatcher.start());
        }
        return text.substring(start);
    }

    public List<String> compareExperience(int profileYears, Integer cvStatedYears, Integer timelineYears) {
        List<String> flags = new ArrayList<>();
        if (cvStatedYears != null && Math.abs(profileYears - cvStatedYears) >= 1) {
            flags.add("Experience mismatch: profile says " + profileYears + " year(s), "
                    + "while the CV states " + cvStatedYears + " year(s). HR should verify.");
        }
        if (timelineYears != null && Math.abs(profileYears - timelineYears) >= 1) {
            flags.add("Possible timeline mismatch: profile says " + profileYears + " year(s), "
                    + "while extracted work-date ranges suggest about " + timelineYears
                    + " year(s). Date parsing can be incomplete; verify manually.");
        }
        return List.copyOf(flags);
    }

    private List<WorkPeriod> extractWorkPeriods(String workHistory) {
        List<WorkPeriod> periods = new ArrayList<>();
        Matcher matcher = DATE_RANGE_PATTERN.matcher(workHistory);
        while (matcher.find()) {
            int startMonth = monthNumber(matcher.group(1), 1);
            YearMonth start = YearMonth.of(Integer.parseInt(matcher.group(2)), startMonth);
            String endYearText = matcher.group(4);
            boolean current = !endYearText.matches("\\d{4}");
            int endMonth = current ? YearMonth.now().getMonthValue() : monthNumber(matcher.group(3), 12);
            int endYear = current ? YearMonth.now().getYear() : Integer.parseInt(endYearText);
            YearMonth end = YearMonth.of(endYear, endMonth);
            if (start.isAfter(end)) {
                continue;
            }
            int lineStart = workHistory.lastIndexOf('\n', matcher.start());
            lineStart = lineStart < 0 ? 0 : lineStart + 1;
            int lineEnd = workHistory.indexOf('\n', matcher.end());
            lineEnd = lineEnd < 0 ? workHistory.length() : lineEnd;
            String context = workHistory.substring(lineStart, lineEnd).replaceAll("\\s+", " ").trim();
            periods.add(new WorkPeriod(start, end, context, current));
        }
        return periods.stream().sorted(Comparator.comparing(WorkPeriod::start)).toList();
    }

    private int monthNumber(String month, int defaultMonth) {
        if (month == null) {
            return defaultMonth;
        }
        return switch (month.substring(0, 3).toLowerCase(Locale.ROOT)) {
            case "jan" -> 1;
            case "feb" -> 2;
            case "mar" -> 3;
            case "apr" -> 4;
            case "may" -> 5;
            case "jun" -> 6;
            case "jul" -> 7;
            case "aug" -> 8;
            case "sep" -> 9;
            case "oct" -> 10;
            case "nov" -> 11;
            case "dec" -> 12;
            default -> defaultMonth;
        };
    }

    private Integer calculateTimelineExperience(List<WorkPeriod> periods) {
        if (periods.isEmpty()) {
            return null;
        }
        List<MonthRange> ranges = periods.stream()
                .map(period -> new MonthRange(period.start(), period.end().plusMonths(1)))
                .sorted(Comparator.comparing(MonthRange::start))
                .toList();
        YearMonth mergedStart = ranges.get(0).start();
        YearMonth mergedEnd = ranges.get(0).endExclusive();
        long totalMonths = 0;
        for (int index = 1; index < ranges.size(); index++) {
            MonthRange range = ranges.get(index);
            if (!range.start().isAfter(mergedEnd)) {
                if (range.endExclusive().isAfter(mergedEnd)) {
                    mergedEnd = range.endExclusive();
                }
            } else {
                totalMonths += ChronoUnit.MONTHS.between(mergedStart, mergedEnd);
                mergedStart = range.start();
                mergedEnd = range.endExclusive();
            }
        }
        totalMonths += ChronoUnit.MONTHS.between(mergedStart, mergedEnd);
        return Math.toIntExact(totalMonths / 12);
    }

    private List<String> buildCareerInsights(
            String text,
            List<WorkPeriod> workPeriods,
            List<String> extractedSkills,
            Integer timelineExperience) {
        List<String> insights = new ArrayList<>();
        if (workPeriods.isEmpty()) {
            insights.add("Work-history dates and employer details could not be reliably extracted; review the CV manually.");
        } else {
            WorkPeriod first = workPeriods.get(0);
            insights.add("Earliest work-history date found: " + first.start() + ". Employer identity needs manual confirmation.");
            workPeriods.stream().skip(Math.max(0, workPeriods.size() - 8L)).forEach(period -> insights.add(
                    (period.current() ? "Possible current role: " : "Past role date range: ")
                            + period.start() + " to " + (period.current() ? "Present" : period.end())
                            + (period.context().isBlank() ? "" : " — " + abbreviate(period.context(), 180))));
            insights.add("Approximate experience from non-overlapping work date ranges: "
                    + timelineExperience + " year(s); date-range extraction may be incomplete.");
        }
        Integer statedExperience = extractExperienceYears(text);
        if (statedExperience != null) {
            insights.add("CV-stated total experience: " + statedExperience + " year(s).");
        }

        List<String> frequentSkills = extractedSkills.stream()
                .sorted(Comparator.<String>comparingInt(skill -> countSkillMentions(text, skill))
                        .reversed()
                        .thenComparing(String::compareToIgnoreCase))
                .limit(5)
                .toList();
        if (!frequentSkills.isEmpty()) {
            insights.add("Frequently mentioned supported skills (mention count is not proficiency): "
                    + String.join(", ", frequentSkills) + ".");
        }

        List<String> fields = FIELD_SIGNALS.stream()
                .map(signal -> Map.entry(signal.name(), signal.terms().stream()
                        .filter(term -> containsTerm(text, term))
                        .toList()))
                .filter(entry -> !entry.getValue().isEmpty())
                .sorted(Comparator.<Map.Entry<String, List<String>>>comparingInt(entry -> entry.getValue().size())
                        .reversed())
                .limit(3)
                .map(entry -> entry.getKey() + " (signals: " + String.join(", ", entry.getValue()) + ")")
                .toList();
        if (fields.isEmpty()) {
            insights.add("Career field could not be determined from configured keyword signals.");
        } else {
            insights.add("Possible career field(s) from CV keywords: " + String.join("; ", fields)
                    + ". This is a keyword hint, not a verified background.");
        }
        insights.add("English speaking/writing proficiency is not assessed from a CV; verify separately if required.");
        return List.copyOf(insights);
    }

    private int countSkillMentions(String text, String skill) {
        Pattern pattern = Pattern.compile("(?i)(?<![a-z0-9])" + Pattern.quote(skill) + "(?![a-z0-9])");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String abbreviate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private boolean containsTerm(String text, String term) {
        return Pattern.compile("(?i)(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])")
                .matcher(text)
                .find();
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
            Integer timelineExperienceYears,
            int confidence,
            String summary,
            List<String> reviewFlags,
            List<String> careerInsights
    ) {
    }

    private record FieldSignal(String name, List<String> terms) {
    }

    private record WorkPeriod(YearMonth start, YearMonth end, String context, boolean current) {
    }

    private record MonthRange(YearMonth start, YearMonth endExclusive) {
    }
}
