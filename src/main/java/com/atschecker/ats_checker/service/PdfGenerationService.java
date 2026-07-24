package com.atschecker.ats_checker.service;

import com.atschecker.ats_checker.entity.AtsResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfGenerationService {

    public byte[] generateReportPdf(AtsResult result) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Fetch standard fonts
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontHandwritten = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // 1. Draw Ruled Lines (Light Blue)
                contentStream.setStrokingColor(180 / 255f, 220 / 255f, 240 / 255f);
                contentStream.setLineWidth(0.8f);
                float startY = 800f;
                float bottomY = 50f;
                float lineSpacing = 24f;
                for (float y = startY - 40; y >= bottomY; y -= lineSpacing) {
                    contentStream.moveTo(30f, y);
                    contentStream.lineTo(565f, y);
                    contentStream.stroke();
                }

                // 2. Draw Left Margin Line (Red)
                contentStream.setStrokingColor(255 / 255f, 100 / 255f, 100 / 255f);
                contentStream.setLineWidth(1.5f);
                contentStream.moveTo(90f, startY);
                contentStream.lineTo(90f, bottomY);
                contentStream.stroke();

                // 3. Draw Examiner Score Stamp (Top Right)
                drawScoreStamp(contentStream, result.getScore(), result.getGrade(), fontBold, fontHandwritten);

                // 4. Header Information (Ruled spacing alignment)
                contentStream.setNonStrokingColor(0, 0, 0); // Black ink for student context
                writeText(contentStream, "REPORT CARD: ATS EVALUATION", 100f, 755f, fontBold, 18);
                
                contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                writeText(contentStream, "STUDENT (RESUME): " + result.getResume().getFilename(), 100f, 725f, fontRegular, 10);
                String specName = result.getSpecialization() != null ? result.getSpecialization().getName() : "General Specialization";
                writeText(contentStream, "EXAM SUBJECT (ROLE): " + specName, 100f, 701f, fontRegular, 10);

                // Section Scores Table
                writeText(contentStream, "SECTION MARKS:", 100f, 653f, fontBold, 12);

                int tableY = 629;
                writeText(contentStream, "Keyword Coverage:", 100f, tableY, fontRegular, 10);
                writeText(contentStream, String.format("%.1f%%", result.getKeywordCoverage()), 240f, tableY, fontBold, 10);

                writeText(contentStream, "Experience Match:", 100f, tableY - 24, fontRegular, 10);
                writeText(contentStream, result.getExperienceMatch() + " / 100", 240f, tableY - 24, fontBold, 10);

                writeText(contentStream, "Education Match:", 100f, tableY - 48, fontRegular, 10);
                writeText(contentStream, result.getEducationMatch() + " / 100", 240f, tableY - 48, fontBold, 10);

                writeText(contentStream, "Projects Match:", 100f, tableY - 72, fontRegular, 10);
                writeText(contentStream, result.getProjectsMatch() + " / 100", 240f, tableY - 72, fontBold, 10);

                writeText(contentStream, "Certifications:", 100f, tableY - 96, fontRegular, 10);
                writeText(contentStream, result.getCertificationsCount() + " Found", 240f, tableY - 96, fontBold, 10);

                // 5. Skills Matching Details
                writeText(contentStream, "KEYWORDS EVALUATION:", 100f, 485f, fontBold, 12);

                contentStream.setNonStrokingColor(0, 128 / 255f, 0); // Green for match
                writeText(contentStream, "✔ MATCHED SKILLS:", 100f, 461f, fontBold, 10);
                
                contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                List<String> matchedLines = wrapText(result.getMatchingSkills(), 450, fontRegular, 9);
                float currY = 437f;
                for (int i = 0; i < Math.min(matchedLines.size(), 3); i++) {
                    writeText(contentStream, matchedLines.get(i), 100f, currY, fontRegular, 9);
                    currY -= 24f;
                }

                contentStream.setNonStrokingColor(200 / 255f, 0, 0); // Red for missing
                writeText(contentStream, "✘ MISSING SKILLS:", 100f, currY + 8f, fontBold, 10);
                
                contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                List<String> missingLines = wrapText(result.getMissingSkills(), 450, fontRegular, 9);
                currY -= 16f;
                for (int i = 0; i < Math.min(missingLines.size(), 3); i++) {
                    writeText(contentStream, missingLines.get(i), 100f, currY, fontRegular, 9);
                    currY -= 24f;
                }

                // 6. Examiner Handwritten Comments (Blue Ink, Cursive)
                currY -= 8f;
                contentStream.setNonStrokingColor(0 / 255f, 80 / 255f, 200 / 255f); // Teacher's blue ink
                writeText(contentStream, "EXAMINER FEEDBACK / REMARKS:", 100f, currY, fontHandwritten, 11);

                String rawComments = result.getRawComments();
                String commentsToPrint = rawComments;
                try {
                    com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawComments);
                    if (rootNode.has("summary")) {
                        commentsToPrint = rootNode.get("summary").asText();
                    }
                } catch (Exception e) {
                    // Fallback to rawComments if not JSON
                }

                List<String> commentLines = wrapText(commentsToPrint, 450, fontHandwritten, 9);
                currY -= 24f;
                for (String line : commentLines) {
                    if (currY < 60) break; // Avoid writing past bottom margin
                    writeText(contentStream, line, 100f, currY, fontHandwritten, 10);
                    currY -= 24f;
                }

                // 7. Teacher Improvement Suggestions
                contentStream.setNonStrokingColor(200 / 255f, 0, 0); // Red ink
                writeText(contentStream, "REVISION TIPS (SUGGESTIONS):", 100f, currY, fontBold, 11);

                contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                currY -= 24f;
                String[] suggestions = result.getSuggestions().split("##");
                for (String suggestion : suggestions) {
                    if (suggestion.trim().isEmpty() || currY < 70) continue;
                    writeText(contentStream, "• " + suggestion, 100f, currY, fontRegular, 9);
                    currY -= 24f;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private void writeText(PDPageContentStream cs, String text, float x, float y, PDFont font, int size) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawScoreStamp(PDPageContentStream cs, int score, String grade, PDFont fontBold, PDFont fontHandwritten) throws IOException {
        // Red stamp color
        cs.setStrokingColor(220 / 255f, 50 / 255f, 50 / 255f);
        cs.setNonStrokingColor(220 / 255f, 50 / 255f, 50 / 255f);
        cs.setLineWidth(2f);

        // Draw double stamp border (square/rectangle)
        // Outer box
        cs.addRect(440, 680, 110, 80);
        cs.stroke();

        // Inner box
        cs.setLineWidth(1f);
        cs.addRect(444, 684, 102, 72);
        cs.stroke();

        // Write "SCORE"
        writeText(cs, "SCORE", 475f, 736f, fontBold, 10);
        
        // Write actual score value e.g. "82/100"
        writeText(cs, score + " / 100", 465f, 716f, fontHandwritten, 14);

        // Write grade value e.g. "GRADE: A+"
        writeText(cs, "GRADE: " + grade, 460f, 696f, fontBold, 11);
    }

    private List<String> wrapText(String text, float width, PDFont font, float fontSize) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float textWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            if (textWidth > width) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }
}
