package com.egs.rjsf.service.export;

import com.egs.rjsf.entity.Award;
import com.egs.rjsf.entity.FormSubmission;
import com.egs.rjsf.repository.AwardRepository;
import com.egs.rjsf.repository.FormSubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates PDF and HTML exports of the composite pre-award review form.
 * Files are stored in a temp export directory and looked up by award number.
 */
@Service
public class FormExportService {

    private static final Logger log = LoggerFactory.getLogger(FormExportService.class);

    private final AwardRepository awardRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final ObjectMapper objectMapper;
    private final Path exportDir;

    private static final List<SectionDef> SECTIONS = List.of(
            new SectionDef("Pre-Award Overview", List.of(
                    "pi_budget", "final_recommended_budget", "funding_source", "negotiation_status",
                    "prime_award_type", "program_manager", "co_principal_investigator",
                    "contract_grants_specialist", "branch_chief", "pi_notification_date", "overview_notes")),
            new SectionDef("A. Safety Requirements Review", List.of(
                    "safety_q1", "programmatic_rec", "safety_q2", "safety_q3", "safety_q4",
                    "safety_q5", "safety_q6", "safety_q7", "safety_q8", "safety_notes")),
            new SectionDef("B. Animal Research Review", List.of(
                    "animal_q1", "animal_q2", "animal_q3", "animal_q4", "iacuc_protocol_number",
                    "animal_q5", "animal_start_date", "animal_notes")),
            new SectionDef("C. Human Research Review", List.of(
                    "no_review_default_no", "human_s1_q1", "human_s1_q2", "human_s1_q3",
                    "human_s1_q4", "human_s1_q5", "human_s1_notes",
                    "has_default_no", "human_has_q1", "human_has_q2", "human_has_q3",
                    "human_has_q4", "human_has_q5", "human_has_q6", "human_has_q7", "human_has_notes",
                    "human_ds_q1", "human_ds_notes",
                    "human_hs_q1", "human_hs_q2", "ct_fda_q1", "ct_nonus_q1", "human_hs_notes",
                    "human_ost_q1", "human_ost_notes", "estimated_start_date")),
            new SectionDef("D. Acquisition/Contracting Review", List.of(
                    "acq_personnel_qualifications", "acq_personnel_effort", "acq_personnel_salary_cap",
                    "acq_personnel_fringe_rate", "acq_personnel_notes",
                    "acq_equip_included", "acq_equip_necessary", "acq_equip_cost_appropriate", "acq_equip_notes",
                    "acq_travel_included", "acq_travel_appropriate", "acq_travel_notes",
                    "acq_materials_included", "acq_materials_appropriate", "acq_materials_cost_appropriate", "acq_materials_notes",
                    "acq_consultant_included", "acq_consultant_necessary", "acq_consultant_duties_described",
                    "acq_consultant_costs_appropriate", "acq_consultant_notes",
                    "acq_third_party_included", "acq_third_party_value_added", "acq_third_party_work_described",
                    "acq_third_party_budget_concerns", "acq_third_party_notes",
                    "acq_other_direct_included", "acq_other_direct_justified", "acq_other_direct_breakdown", "acq_other_direct_notes",
                    "acq_additional_has_concerns", "acq_additional_notes",
                    "acq_peer_review_score", "acq_peer_review_outcome", "acq_peer_comments",
                    "acq_sow_comments",
                    "acq_cps_received", "acq_cps_foreign_influence", "acq_cps_overlap_identified", "acq_cps_comments",
                    "acq_ier_applicable", "acq_ier_comment", "acq_ier_plan_included", "acq_ier_plan_notes",
                    "acq_dmp_received", "acq_dmp_repository_identified", "acq_dmp_sharing_timeline", "acq_dmp_notes")),
            new SectionDef("Final Recommendation to Award", List.of(
                    "scientific_overlap", "foreign_involvement", "risg_approval",
                    "so_recommendation", "so_comments", "gor_recommendation", "gor_comments"))
    );

    private static final Map<String, String> FIELD_LABELS = buildFieldLabels();

    public FormExportService(AwardRepository awardRepository,
                             FormSubmissionRepository formSubmissionRepository,
                             ObjectMapper objectMapper) {
        this.awardRepository = awardRepository;
        this.formSubmissionRepository = formSubmissionRepository;
        this.objectMapper = objectMapper;
        this.exportDir = Path.of(System.getProperty("java.io.tmpdir"), "rjsf-exports");
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            log.warn("Could not create export directory: {}", exportDir, e);
        }
    }

    public String generateExports(UUID awardId) {
        Award award = awardRepository.findById(awardId)
                .orElseThrow(() -> new EntityNotFoundException("Award not found: " + awardId));

        FormSubmission submission = formSubmissionRepository
                .findByAwardIdOrderByFormKey(awardId).stream()
                .filter(s -> "pre_award_composite".equals(s.getFormKey()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No composite submission for award: " + awardId));

        String awardNumber = award.getAwardNumber() != null ? award.getAwardNumber() : award.getLogNumber();
        String safeName = awardNumber.replaceAll("[^a-zA-Z0-9_-]", "_");
        Map<String, Object> formData = submission.getFormData();

        // Generate HTML
        String html = buildHtml(award, formData);
        Path htmlPath = exportDir.resolve(safeName + ".html");
        try {
            Files.writeString(htmlPath, html, StandardCharsets.UTF_8);
            log.info("Exported HTML to {}", htmlPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write HTML export", e);
        }

        // Generate PDF using PDFBox
        Path pdfPath = exportDir.resolve(safeName + ".pdf");
        try {
            generatePdf(award, formData, pdfPath);
            log.info("Exported PDF to {}", pdfPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write PDF export", e);
        }

        return safeName;
    }

    public Path getPdfPath(String awardNumber) {
        String safeName = awardNumber.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path path = exportDir.resolve(safeName + ".pdf");
        return Files.exists(path) ? path : null;
    }

    public Path getHtmlPath(String awardNumber) {
        String safeName = awardNumber.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path path = exportDir.resolve(safeName + ".html");
        return Files.exists(path) ? path : null;
    }

    // ---- PDF generation with PDFBox ----

    private static final float PDF_MARGIN = 40;
    private static final float PDF_FONT_SIZE = 8;
    private static final float PDF_LINE_HEIGHT = 11;
    private static final float PDF_COL_GAP = 8;

    @SuppressWarnings("deprecation")
    private void generatePdf(Award award, Map<String, Object> formData, Path pdfPath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            float yStart = PDRectangle.LETTER.getHeight() - PDF_MARGIN;
            float pageWidth = PDRectangle.LETTER.getWidth() - 2 * PDF_MARGIN;
            float labelColWidth = pageWidth * 0.55f;
            float valueColWidth = pageWidth * 0.45f;
            float valueColX = PDF_MARGIN + labelColWidth + PDF_COL_GAP;
            float[] yPos = {yStart};
            PDPage[] currentPage = {new PDPage(PDRectangle.LETTER)};
            doc.addPage(currentPage[0]);
            PDPageContentStream[] cs = {new PDPageContentStream(doc, currentPage[0])};

            // Start a new page if needed, returns true if a new page was created
            Runnable newPageIfNeeded = () -> {
                if (yPos[0] < PDF_MARGIN + 30) {
                    try {
                        cs[0].close();
                        currentPage[0] = new PDPage(PDRectangle.LETTER);
                        doc.addPage(currentPage[0]);
                        cs[0] = new PDPageContentStream(doc, currentPage[0]);
                        yPos[0] = yStart;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            // Title
            cs[0].beginText();
            cs[0].setFont(PDType1Font.HELVETICA_BOLD, 14);
            cs[0].newLineAtOffset(PDF_MARGIN, yPos[0]);
            cs[0].showText("Pre-Award / Negotiations Review");
            cs[0].endText();
            yPos[0] -= 16;

            // Award info
            String info = "Log: " + nvl(award.getLogNumber());
            if (award.getAwardNumber() != null) info += "  |  Award: " + award.getAwardNumber();
            if (award.getPrincipalInvestigator() != null) info += "  |  PI: " + award.getPrincipalInvestigator();
            cs[0].beginText();
            cs[0].setFont(PDType1Font.HELVETICA, 8);
            cs[0].newLineAtOffset(PDF_MARGIN, yPos[0]);
            cs[0].showText(info);
            cs[0].endText();
            yPos[0] -= 10;

            cs[0].beginText();
            cs[0].setFont(PDType1Font.HELVETICA, 7);
            cs[0].newLineAtOffset(PDF_MARGIN, yPos[0]);
            cs[0].showText("Generated: " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            cs[0].endText();
            yPos[0] -= 16;

            // Sections
            for (SectionDef section : SECTIONS) {
                newPageIfNeeded.run();

                // Section header
                cs[0].beginText();
                cs[0].setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs[0].newLineAtOffset(PDF_MARGIN, yPos[0]);
                cs[0].showText(section.title);
                cs[0].endText();
                yPos[0] -= 3;

                cs[0].setLineWidth(0.5f);
                cs[0].moveTo(PDF_MARGIN, yPos[0]);
                cs[0].lineTo(PDF_MARGIN + pageWidth, yPos[0]);
                cs[0].stroke();
                yPos[0] -= 12;

                // Column headers
                cs[0].setNonStrokingColor(0.26f, 0.55f, 0.79f); // #428bca
                cs[0].addRect(PDF_MARGIN, yPos[0] - 2, pageWidth, 12);
                cs[0].fill();
                cs[0].setNonStrokingColor(1f, 1f, 1f);
                cs[0].beginText();
                cs[0].setFont(PDType1Font.HELVETICA_BOLD, 8);
                cs[0].newLineAtOffset(PDF_MARGIN + 4, yPos[0] + 1);
                cs[0].showText("Question / Field");
                cs[0].endText();
                cs[0].beginText();
                cs[0].newLineAtOffset(valueColX, yPos[0] + 1);
                cs[0].showText("Answer / Value");
                cs[0].endText();
                cs[0].setNonStrokingColor(0f, 0f, 0f);
                yPos[0] -= 14;

                // Field rows
                boolean alternate = false;
                for (String field : section.fields) {
                    String label = FIELD_LABELS.getOrDefault(field, humanize(field));
                    Object value = formData.get(field);
                    String display = formatValue(value);

                    // Word-wrap both columns
                    List<String> labelLines = wrapText(label, PDType1Font.HELVETICA_BOLD, PDF_FONT_SIZE, labelColWidth - 8);
                    List<String> valueLines = wrapText(display, PDType1Font.HELVETICA, PDF_FONT_SIZE, valueColWidth - 8);
                    int rowLines = Math.max(labelLines.size(), valueLines.size());
                    float rowHeight = rowLines * PDF_LINE_HEIGHT + 4;

                    // Page break if this row won't fit
                    if (yPos[0] - rowHeight < PDF_MARGIN + 20) {
                        cs[0].close();
                        currentPage[0] = new PDPage(PDRectangle.LETTER);
                        doc.addPage(currentPage[0]);
                        cs[0] = new PDPageContentStream(doc, currentPage[0]);
                        yPos[0] = yStart;
                    }

                    // Alternating row background
                    if (alternate) {
                        cs[0].setNonStrokingColor(0.96f, 0.97f, 0.99f); // light blue-gray
                        cs[0].addRect(PDF_MARGIN, yPos[0] - rowHeight + PDF_LINE_HEIGHT - 2, pageWidth, rowHeight);
                        cs[0].fill();
                        cs[0].setNonStrokingColor(0f, 0f, 0f);
                    }
                    alternate = !alternate;

                    // Draw row border
                    cs[0].setStrokingColor(0.87f, 0.87f, 0.87f);
                    cs[0].setLineWidth(0.3f);
                    cs[0].addRect(PDF_MARGIN, yPos[0] - rowHeight + PDF_LINE_HEIGHT - 2, pageWidth, rowHeight);
                    cs[0].stroke();
                    // Vertical divider
                    cs[0].moveTo(valueColX - PDF_COL_GAP / 2, yPos[0] + PDF_LINE_HEIGHT - 2);
                    cs[0].lineTo(valueColX - PDF_COL_GAP / 2, yPos[0] - rowHeight + PDF_LINE_HEIGHT - 2);
                    cs[0].stroke();
                    cs[0].setStrokingColor(0f, 0f, 0f);

                    // Draw label lines
                    float lineY = yPos[0];
                    for (String line : labelLines) {
                        cs[0].beginText();
                        cs[0].setFont(PDType1Font.HELVETICA_BOLD, PDF_FONT_SIZE);
                        cs[0].newLineAtOffset(PDF_MARGIN + 4, lineY);
                        cs[0].showText(line);
                        cs[0].endText();
                        lineY -= PDF_LINE_HEIGHT;
                    }

                    // Draw value lines
                    lineY = yPos[0];
                    for (String line : valueLines) {
                        cs[0].beginText();
                        cs[0].setFont(PDType1Font.HELVETICA, PDF_FONT_SIZE);
                        cs[0].newLineAtOffset(valueColX, lineY);
                        cs[0].showText(line);
                        cs[0].endText();
                        lineY -= PDF_LINE_HEIGHT;
                    }

                    yPos[0] -= rowHeight;
                }
                yPos[0] -= 10;
            }

            // Footer
            newPageIfNeeded.run();
            cs[0].beginText();
            cs[0].setFont(PDType1Font.HELVETICA, 7);
            cs[0].newLineAtOffset(PDF_MARGIN, PDF_MARGIN);
            cs[0].showText("Generated by RJSF Pre-Award Review System");
            cs[0].endText();

            cs[0].close();
            doc.save(pdfPath.toFile());
        }
    }

    /** Word-wrap text to fit within a given width using the specified font and size. */
    @SuppressWarnings("deprecation")
    private static List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) {
        if (text == null || text.isBlank()) return List.of("");
        List<String> lines = new ArrayList<>();
        // Handle multi-line values (e.g., JSON pretty-printed)
        for (String paragraph : text.split("\\n")) {
            String[] words = paragraph.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String test = current.isEmpty() ? word : current + " " + word;
                try {
                    float width = font.getStringWidth(test) / 1000 * fontSize;
                    if (width > maxWidth && !current.isEmpty()) {
                        lines.add(current.toString());
                        current = new StringBuilder(word);
                    } else {
                        current = new StringBuilder(test);
                    }
                } catch (IOException e) {
                    current = new StringBuilder(test);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    // ---- HTML generation ----

    private String buildHtml(Award award, Map<String, Object> formData) {
        StringBuilder sb = new StringBuilder();
        String generatedAt = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\"/>\n");
        sb.append("<title>Pre-Award Review — ").append(esc(award.getLogNumber())).append("</title>\n");
        sb.append("<style>\n");
        sb.append("body { font-family: 'Ubuntu', Arial, sans-serif; font-size: 12px; color: #2C3E50; margin: 30px; }\n");
        sb.append("h1 { font-size: 20px; color: #2C3E50; border-bottom: 2px solid #428bca; padding-bottom: 6px; }\n");
        sb.append("h2 { font-size: 15px; color: #428bca; margin-top: 20px; border-bottom: 1px solid #ddd; padding-bottom: 4px; }\n");
        sb.append("table { width: 100%; border-collapse: collapse; margin-bottom: 14px; }\n");
        sb.append("th { background-color: #428bca; color: #fff; text-align: left; padding: 5px 8px; font-size: 12px; }\n");
        sb.append("td { border: 1px solid #ddd; padding: 4px 8px; font-size: 12px; vertical-align: top; }\n");
        sb.append("tr:nth-child(even) td { background-color: #f4f8fc; }\n");
        sb.append(".label { font-weight: bold; width: 40%; }\n");
        sb.append(".value { width: 60%; }\n");
        sb.append(".header-info { font-size: 12px; color: #a6a6a8; margin-bottom: 16px; }\n");
        sb.append(".footer { margin-top: 30px; font-size: 10px; color: #a6a6a8; border-top: 1px solid #ddd; padding-top: 6px; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<h1>Pre-Award / Negotiations Review</h1>\n");
        sb.append("<div class=\"header-info\">");
        sb.append("Log Number: <strong>").append(esc(award.getLogNumber())).append("</strong>");
        if (award.getAwardNumber() != null)
            sb.append(" &nbsp;|&nbsp; Award Number: <strong>").append(esc(award.getAwardNumber())).append("</strong>");
        if (award.getPrincipalInvestigator() != null)
            sb.append(" &nbsp;|&nbsp; PI: <strong>").append(esc(award.getPrincipalInvestigator())).append("</strong>");
        sb.append(" &nbsp;|&nbsp; Generated: ").append(generatedAt);
        sb.append("</div>\n");

        for (SectionDef section : SECTIONS) {
            sb.append("<h2>").append(esc(section.title)).append("</h2>\n");
            sb.append("<table>\n");
            for (String field : section.fields) {
                Object value = formData.get(field);
                String display = formatValue(value);
                String label = FIELD_LABELS.getOrDefault(field, humanize(field));
                sb.append("<tr><td class=\"label\">").append(esc(label)).append("</td>");
                sb.append("<td class=\"value\">").append(esc(display)).append("</td></tr>\n");
            }
            sb.append("</table>\n");
        }

        sb.append("<div class=\"footer\">Generated by RJSF Pre-Award Review System on ").append(generatedAt).append("</div>\n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value == null) return "";
        if (value instanceof List || value instanceof Map) {
            try { return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value); }
            catch (Exception e) { return value.toString(); }
        }
        return value.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String nvl(String s) { return s != null ? s : ""; }



    private static String humanize(String fieldKey) {
        String s = fieldKey.replace("_", " ");
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private record SectionDef(String title, List<String> fields) {}

    private static Map<String, String> buildFieldLabels() {
        Map<String, String> m = new LinkedHashMap<>();

        // Overview
        m.put("pi_budget", "PI Budget");
        m.put("final_recommended_budget", "Final Recommended Budget");
        m.put("funding_source", "Funding Source");
        m.put("negotiation_status", "Negotiation Status");
        m.put("prime_award_type", "Prime Award (Intra/Extra)");
        m.put("program_manager", "Program Manager");
        m.put("co_principal_investigator", "Co-Principal Investigator");
        m.put("contract_grants_specialist", "Contract/Grants Specialist");
        m.put("branch_chief", "Branch Chief");
        m.put("pi_notification_date", "PI Notification Date");
        m.put("overview_notes", "Overview Notes");

        // Safety
        m.put("safety_q1", "1. Programmatic Record of Environmental Compliance (REC) available?");
        m.put("programmatic_rec", "Selected Programmatic REC");
        m.put("safety_q2", "2. Involves Army-provided infectious agents?");
        m.put("safety_q3", "3. Involves Biological Select Agents or Toxins (BSAT)?");
        m.put("safety_q4", "4. Involves specific chemical agents?");
        m.put("safety_q5", "5. Involves pesticides outside of established lab?");
        m.put("safety_q6", "6. Potential likelihood of significant negative effects on public health, safety, or environment?");
        m.put("safety_q7", "7. Does the project involve ionizing radiation or radioactive materials?");
        m.put("safety_q8", "8. Has a NEPA compliance determination been completed?");
        m.put("safety_notes", "Note");

        // Animal
        m.put("animal_q1", "1. Animals used?");
        m.put("animal_q2", "2. Will any DOD-funded animal studies be performed at a site outside the US?");
        m.put("animal_q3", "3. Will any DOD-funded animal studies be performed at a site in a foreign country of concern (FCOC)?");
        m.put("animal_q4", "4. Has a valid IACUC protocol approval been obtained or submitted?");
        m.put("iacuc_protocol_number", "IACUC Protocol Number (if available)");
        m.put("animal_q5", "5. Does this project involve endangered or protected species?");
        m.put("animal_start_date", "6. Estimated Animal Research Start (if known)");
        m.put("animal_notes", "Notes");

        // Human - No Regulatory Review
        m.put("no_review_default_no", "Default all to NO");
        m.put("human_s1_q1", "1. Commercially available human cell lines, including cadaveric and HEK293 cells");
        m.put("human_s1_q2", "2. Commercially available human organoids");
        m.put("human_s1_q3", "3. Commercially available POOLED human products");
        m.put("human_s1_q4", "4. Established, existing patient-derived xenograft (PDX) models");
        m.put("human_s1_q5", "5. Commercial services");
        m.put("human_s1_notes", "Note");

        // Human - Anatomical Substances
        m.put("has_default_no", "Default all to NO");
        m.put("human_has_q1", "1. Collecting human specimens prospectively for research purposes");
        m.put("human_has_q2", "2. Human cell lines that cannot be purchased from a vendor");
        m.put("human_has_q3", "3. Commercially available human anatomical substances (non-pooled)");
        m.put("human_has_q4", "4. Creation of new patient-derived xenograft (PDX) models from human tissue samples");
        m.put("human_has_q5", "5. Using human specimens obtained through clinical trials, research studies, collaborations, biobanks, or clinical departments");
        m.put("human_has_q6", "6. Cadavers or post-mortem human specimens");
        m.put("human_has_q7", "7. Unique or regulated sample types, e.g., human embryonic stem cell lines");
        m.put("human_has_notes", "Note");

        // Human - Data Secondary Use
        m.put("human_ds_q1", "1. Does the project involve secondary use of human data?");
        m.put("human_ds_notes", "Note");

        // Human - Subjects
        m.put("human_hs_q1", "1. Interaction/intervention with human subjects?");
        m.put("human_hs_q2", "2. A Clinical trial?");
        m.put("ct_fda_q1", "a. Is it FDA regulated?");
        m.put("ct_nonus_q1", "b. Will any clinical trial sites be located outside the US?");
        m.put("human_hs_notes", "Note");

        // Human - Special Topics
        m.put("human_ost_q1", "1. Situations requiring additional Human Research Regulatory Agency/DOD review?");
        m.put("human_ost_notes", "Note");

        // Human - Estimated Start
        m.put("estimated_start_date", "1. Estimated Human Research Start Date (if known)");

        // Acquisition - Personnel
        m.put("acq_personnel_qualifications", "i. Are the type/qualifications of proposed personnel appropriate?");
        m.put("acq_personnel_effort", "ii. Is the level of effort of proposed personnel appropriate?");
        m.put("acq_personnel_salary_cap", "iii. Are proposed salaries within the applicable salary cap?");
        m.put("acq_personnel_fringe_rate", "iv. Are the proposed fringe benefit rates reasonable?");
        m.put("acq_personnel_notes", "Note");

        // Acquisition - Equipment
        m.put("acq_equip_included", "i. Are any equipment costs included in the proposed budget?");
        m.put("acq_equip_necessary", "ii. Is the equipment necessary to conduct the project?");
        m.put("acq_equip_cost_appropriate", "iii. In general, does the cost appear to be appropriate?");
        m.put("acq_equip_notes", "Note");

        // Acquisition - Travel
        m.put("acq_travel_included", "i. Are funds for travel included in the proposed budget?");
        m.put("acq_travel_appropriate", "ii. Are the number and type(s) of trip(s) appropriate for the project?");
        m.put("acq_travel_notes", "Note");

        // Acquisition - Materials
        m.put("acq_materials_included", "i. Are funds for materials, supplies and consumables included?");
        m.put("acq_materials_appropriate", "ii. Are the types and quantities of proposed items appropriate?");
        m.put("acq_materials_cost_appropriate", "iii. Do the costs appear to be appropriate?");
        m.put("acq_materials_notes", "Note");

        // Acquisition - Consultant
        m.put("acq_consultant_included", "i. Are funds for consultant(s)/collaborator(s) included?");
        m.put("acq_consultant_necessary", "ii. Is the proposed consultant(s)/collaborator(s) necessary?");
        m.put("acq_consultant_duties_described", "iii. Are the duties sufficiently described?");
        m.put("acq_consultant_costs_appropriate", "iv. Do the costs/fees appear to be appropriate?");
        m.put("acq_consultant_notes", "Note");

        // Acquisition - Third Party
        m.put("acq_third_party_included", "i. Are funds for a 3rd party included in the proposed budget?");
        m.put("acq_third_party_value_added", "ii. Is the 3rd party providing value added to the project?");
        m.put("acq_third_party_work_described", "iii. Is the work to be performed by the site sufficiently described?");
        m.put("acq_third_party_budget_concerns", "iv. Are there any concerns with the associated budget?");
        m.put("acq_third_party_notes", "Note");

        // Acquisition - Other Direct Costs
        m.put("acq_other_direct_included", "i. Are funds for Other Direct Costs included in the proposed budget?");
        m.put("acq_other_direct_justified", "ii. Are the costs necessary and/or fully justified?");
        m.put("acq_other_direct_breakdown", "iii. Is the breakdown sufficient?");
        m.put("acq_other_direct_notes", "Note");

        // Acquisition - Additional
        m.put("acq_additional_has_concerns", "Do you have any other budget-related concerns?");
        m.put("acq_additional_notes", "Note");

        // Acquisition - Peer Review
        m.put("acq_peer_review_score", "Overall Review Score (if applicable)");
        m.put("acq_peer_review_outcome", "Review Recommendation");
        m.put("acq_peer_comments", "Note (Required)");

        // Acquisition - SOW
        m.put("acq_sow_comments", "Note (Required)");

        // Acquisition - CPS
        m.put("acq_cps_received", "a. Has an updated and certified CPS document been received?");
        m.put("acq_cps_foreign_influence", "b. Has foreign influence screening been completed?");
        m.put("acq_cps_overlap_identified", "c. Has any scientific, budgetary, or commitment overlap been identified?");
        m.put("acq_cps_comments", "Note (Required)");

        // Acquisition - IER
        m.put("acq_ier_applicable", "a. Is the IER requirement applicable for this project?");
        m.put("acq_ier_comment", "Note (required if Unclear)");
        m.put("acq_ier_plan_included", "b. Was a Planned IER Report Included with the proposal?");
        m.put("acq_ier_plan_notes", "Note");

        // Acquisition - Data Management
        m.put("acq_dmp_received", "a. Has an acceptable Data Management Plan been received?");
        m.put("acq_dmp_repository_identified", "b. Has a designated data repository been identified?");
        m.put("acq_dmp_sharing_timeline", "c. Is the proposed data sharing timeline consistent with DOD policy?");
        m.put("acq_dmp_notes", "Note");

        // Final Recommendation
        m.put("scientific_overlap", "Was scientific overlap identified during negotiations?");
        m.put("foreign_involvement", "Was this project reported to RISG for any type of foreign involvement during negotiations?");
        m.put("risg_approval", "Does this project have RISG approval to proceed?");
        m.put("so_recommendation", "SO Recommendation");
        m.put("so_comments", "SO Comments");
        m.put("gor_recommendation", "GOR/COR Recommendation");
        m.put("gor_comments", "GOR/COR Comments");

        return m;
    }
}
