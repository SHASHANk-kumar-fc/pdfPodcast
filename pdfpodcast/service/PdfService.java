package com.pdfpodcast.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
public class PdfService {

    @Value("${pdf.max-file-size:52428800}")  // 50MB default
    private long maxFileSize;

    @Value("${pdf.max-text-length:100000}")  // 100k chars
    private int maxTextLength;

    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    public String extractText(MultipartFile file) throws Exception {
        try {
            // Validate file
            validatePdfFile(file);

            log.info("Extracting text from PDF: {}", file.getOriginalFilename());

            try (InputStream inputStream = file.getInputStream()) {
                return extractTextFromStream(inputStream);
            } catch (Exception e) {
                log.error("Error extracting text from PDF: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("PDF processing error: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String extractText(String filePath) throws Exception {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Stored source file not found");
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            return extractTextFromStream(inputStream);
        }
    }

    private String extractTextFromStream(InputStream inputStream) throws Exception {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            if (document.isEncrypted()) {
                log.warn("PDF is encrypted, attempting to extract anyway");
            }

            if (document.getNumberOfPages() == 0) {
                throw new IllegalArgumentException("PDF has no pages");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("PDF does not contain extractable text");
            }

            if (text.length() > maxTextLength) {
                log.warn("Extracted text exceeds max length, truncating from {} to {}", text.length(), maxTextLength);
                text = text.substring(0, maxTextLength);
            }

            log.info("Successfully extracted {} characters from PDF", text.length());
            return text;
        }
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPE.equals(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are allowed");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + maxFileSize + " bytes");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File must be a PDF");
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(5);
            String magic = new String(header);
            if (!magic.startsWith("%PDF-")) {
                throw new IllegalArgumentException("File content is not a valid PDF");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to validate PDF content");
        }

        log.debug("PDF validation passed for file: {}", filename);
    }

    public List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }

        for (int i = 0; i < text.length(); i += chunkSize) {
            int endIndex = Math.min(text.length(), i + chunkSize);
            chunks.add(text.substring(i, endIndex));
        }

        log.info("Text split into {} chunks of size {}", chunks.size(), chunkSize);
        return chunks;
    }
}
