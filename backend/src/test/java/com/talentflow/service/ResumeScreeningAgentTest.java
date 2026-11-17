package com.talentflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class ResumeScreeningAgentTest {

    private final ResumeScreeningAgent agent = new ResumeScreeningAgent();

    @Test
    void rejectsOfferLetterBeforeRegistration() throws IOException {
        MockMultipartFile offerLetter = pdf("offer.pdf",
                "OFFER LETTER We are pleased to offer you employment. Joining date and annual compensation details follow. "
                        + "Please accept this offer by signing the terms of employment. ".repeat(3));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> agent.analyze(offerLetter));

        assertEquals(422, error.getStatusCode().value());
        assertTrue(error.getReason().contains("offer/employment letter"));
    }

    @Test
    void acceptsCvAndExtractsSkillsAndExperience() throws IOException {
        MockMultipartFile resume = pdf("resume.pdf",
                "PROFESSIONAL SUMMARY Software engineer with 5 years of professional experience. "
                        + "TECHNICAL SKILLS Java Spring Boot React SQL. "
                        + "WORK EXPERIENCE Built APIs and applications. EDUCATION Bachelor of Science. "
                        + "PROJECTS and certifications listed below. ".repeat(3));

        ResumeScreeningAgent.ScreeningResult result = agent.analyze(resume);

        assertEquals(5, result.experienceYears());
        assertTrue(result.extractedSkills().containsAll(List.of("Java", "Spring Boot", "React", "SQL")),
                () -> "Unexpected extracted skills: " + result.extractedSkills() + "; extracted text: " + result.extractedText());
        assertTrue(result.confidence() >= 60);
    }

    @Test
    void rejectsPdfWithNoResumeSignals() throws IOException {
        MockMultipartFile unrelated = pdf("document.pdf",
                "This is a general business letter with a date, recipient, greeting, and closing. ".repeat(5));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> agent.analyze(unrelated));

        assertEquals(422, error.getStatusCode().value());
        assertTrue(error.getReason().contains("could not verify this as a CV"));
    }

    @Test
    void flagsProfanityAndSexuallyExplicitTermsForHrReview() throws IOException {
        MockMultipartFile resume = pdf("resume.pdf",
                "PROFESSIONAL SUMMARY Software engineer with 5 years of professional experience. "
                        + "TECHNICAL SKILLS Java Spring Boot React SQL. "
                        + "WORK EXPERIENCE Built APIs and applications. EDUCATION Bachelor of Science. "
                        + "This is an asshole profile containing pornography. ".repeat(3));

        ResumeScreeningAgent.ScreeningResult result = agent.analyze(resume);

        assertTrue(result.reviewFlags().stream().anyMatch(flag -> flag.contains("abusive or profane")));
        assertTrue(result.reviewFlags().stream().anyMatch(flag -> flag.contains("sexually explicit")));
    }

    @Test
    void flagsResumesWithInsufficientlySpecifiedExperienceOrSkills() throws IOException {
        MockMultipartFile resume = pdf("resume.pdf",
                "PROFESSIONAL SUMMARY Experienced team member. WORK EXPERIENCE "
                        + "EDUCATION Bachelor of Arts. PROJECTS listed below. ".repeat(4));

        ResumeScreeningAgent.ScreeningResult result = agent.analyze(resume);

        assertTrue(result.reviewFlags().stream().anyMatch(flag -> flag.contains("may be incomplete")));
    }

    @Test
    void flagsInappropriateRegistrationProfileText() {
        List<String> flags = agent.detectContentIssues(
                "Example",
                "Person",
                "developer@example.com",
                "software engineer",
                "Java and React");

        assertTrue(flags.isEmpty());
        assertTrue(agent.detectContentIssues("asshole").stream()
                .anyMatch(flag -> flag.contains("abusive or profane")));
    }

    private MockMultipartFile pdf(String name, String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.newLineAtOffset(40, 750);
                StringBuilder line = new StringBuilder();
                for (String word : text.split("\\s+")) {
                    if (line.length() + word.length() > 85) {
                        content.showText(line.toString());
                        content.newLineAtOffset(0, -14);
                        line.setLength(0);
                    }
                    if (!line.isEmpty()) {
                        line.append(' ');
                    }
                    line.append(word);
                }
                if (!line.isEmpty()) {
                    content.showText(line.toString());
                }
                content.endText();
            }
            document.save(output);
            return new MockMultipartFile("resume", name, "application/pdf", output.toByteArray());
        }
    }
}
