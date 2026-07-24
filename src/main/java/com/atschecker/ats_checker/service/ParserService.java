package com.atschecker.ats_checker.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ParserService {

    public String parse(String filename, byte[] fileBytes) throws IOException {
        String extension = getFileExtension(filename).toLowerCase();
        return switch (extension) {
            case "pdf" -> parsePdf(fileBytes);
            case "docx" -> parseDocx(fileBytes);
            case "txt" -> parseTxt(fileBytes);
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };
    }

    private String parsePdf(byte[] fileBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(byte[] fileBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
             XWPFDocument document = new XWPFDocument(bais);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String parseTxt(byte[] fileBytes) {
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
