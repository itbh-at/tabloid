package at.itbh.tabloid.rest;

import io.quarkus.test.junit.QuarkusTest;
import java.util.logging.Logger;
import jakarta.ws.rs.core.MediaType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableColumn;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;

import at.itbh.tabloid.util.ColumnWidthHeuristic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TabloidResourceTest {

    private static final Logger LOGGER = Logger.getLogger(TabloidResourceTest.class.getName());

    @Test
    public void testXlsxGeneration() throws IOException {
        String json = loadResource("/test-payload.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        assertNotNull(fileBytes);
        assertTrue(fileBytes.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet1 = workbook.getSheet("Regional Sales Performance");
            assertNotNull(sheet1);

            Row headerRow = sheet1.getRow(0);
            assertNotNull(headerRow);
            Row dataRow = sheet1.getRow(1);
            assertNotNull(dataRow);

            CellStyle headerStyle = headerRow.getCell(0).getCellStyle();
            Font headerFont = workbook.getFontAt(headerStyle.getFontIndex());
            assertTrue(headerFont.getBold(), "Header font should be bold.");
            CellStyle rowHeaderStyle = dataRow.getCell(0).getCellStyle();
            Font rowHeaderFont = workbook.getFontAt(rowHeaderStyle.getFontIndex());
            assertTrue(rowHeaderFont.getBold(), "Row header font should be bold.");
            CellStyle dataStyle = dataRow.getCell(1).getCellStyle();
            Font dataFont = workbook.getFontAt(dataStyle.getFontIndex());
            assertFalse(dataFont.getBold(), "Data font should NOT be bold.");
            assertEquals("Arial", dataFont.getFontName(), "Data font should be Arial.");

            int longHeaderColumnIndex = 2;
            assertTrue(sheet1.getColumnWidth(longHeaderColumnIndex) > (12 * 256),
                    "Column with long header should be auto-sized.");
        }
    }

    @Test
    public void testAlignment() throws IOException {
        String json = loadResource("/test-payload-alignment.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        assertNotNull(fileBytes);
        assertTrue(fileBytes.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet1 = workbook.getSheet("Alignment");
            assertNotNull(sheet1, "Sheet 'Alignment' should exist.");
            Row dataRow = sheet1.getRow(1);
            assertNotNull(dataRow, "Data row should exist.");
            CellStyle leftStyle = dataRow.getCell(0).getCellStyle();
            assertEquals(HorizontalAlignment.LEFT, leftStyle.getAlignment(), "Column 'Left' should be left-aligned.");
            assertEquals(VerticalAlignment.TOP, leftStyle.getVerticalAlignment(),
                    "Column 'Left' should be top-aligned.");
            CellStyle centerStyle = dataRow.getCell(1).getCellStyle();
            assertEquals(HorizontalAlignment.CENTER, centerStyle.getAlignment(),
                    "Column 'Center' should be center-aligned.");
            assertEquals(VerticalAlignment.CENTER, centerStyle.getVerticalAlignment(),
                    "Column 'Center' should be center-aligned.");
            CellStyle rightStyle = dataRow.getCell(2).getCellStyle();
            assertEquals(HorizontalAlignment.RIGHT, rightStyle.getAlignment(),
                    "Column 'Right' should be right-aligned.");
            assertEquals(VerticalAlignment.BOTTOM, rightStyle.getVerticalAlignment(),
                    "Column 'Right' should be bottom-aligned.");
        }
    }

    @Test
    public void testOdsGeneration() throws Exception {
        String json = loadResource("/test-payload.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/vnd.oasis.opendocument.spreadsheet")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        assertNotNull(fileBytes);
        assertTrue(fileBytes.length > 0);

        try (var doc = OdfSpreadsheetDocument.loadDocument(new ByteArrayInputStream(fileBytes))) {
            assertEquals("Q3 2025 Comprehensive Sales Report", doc.getOfficeMetadata().getTitle());
            assertEquals("ITBH Test Suite", doc.getOfficeMetadata().getCreator());

            OdfTable sheet = doc.getTableByName("Regional Sales Performance");
            assertNotNull(sheet, "Sheet 'Regional Sales Performance' should exist.");

            assertEquals("Region", sheet.getCellByPosition(0, 0).getStringValue());

            OdfTableCell cellB2 = sheet.getCellByPosition(1, 1);
            assertEquals("float", cellB2.getValueType());
            assertEquals(5430.0, cellB2.getDoubleValue(), 0.001);

            OdfTableCell cellD2 = sheet.getCellByPosition(3, 1);
            assertEquals("date", cellD2.getValueType());
            assertNotNull(cellD2.getDateValue());
        }
    }

    @Test
    public void testOdsColumnWidthIsDynamic() throws Exception {
        String json = loadResource("/test-payload-long-text.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/vnd.oasis.opendocument.spreadsheet")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (var doc = OdfSpreadsheetDocument.loadDocument(new ByteArrayInputStream(fileBytes))) {
            OdfTable sheet = doc.getTableByName("Application Logs");
            assertNotNull(sheet);

            // Test a column OVER the threshold
            OdfTableColumn logColumn = sheet.getColumnByIndex(2);
            String longestLog = "User login attempt failed for user 'j.doe'. This is a very long log entry designed specifically to test the column width heuristic calculation performance. The string needs to be sufficiently long to force the heuristic to perform meaningful work.";
            int expectedLogWidth = ColumnWidthHeuristic.calculateWidth(longestLog.length());
            long actualLogWidth = logColumn.getWidth();
            String logMessage = "Log Message column should be wide and include the large padding. Expected ~"
                    + expectedLogWidth + " but was " + actualLogWidth;
            assertTrue(Math.abs(expectedLogWidth - actualLogWidth) <= 2, logMessage);

            // Test a column UNDER the threshold
            OdfTableColumn serviceColumn = sheet.getColumnByIndex(1);
            int serviceLength = "AuthenticationService".length();
            int expectedServiceWidth = ColumnWidthHeuristic.calculateWidth(serviceLength);
            long actualServiceWidth = serviceColumn.getWidth();
            String serviceMessage = "Service column should be narrower and include the small padding. Expected ~"
                    + expectedServiceWidth + " but was " + actualServiceWidth;
            assertTrue(Math.abs(expectedServiceWidth - actualServiceWidth) <= 2, serviceMessage);
        }
    }

    @Test
    public void testOdsColumnWidthHeuristic() throws Exception {
        String json = loadResource("/test-payload.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/vnd.oasis.opendocument.spreadsheet")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (var doc = OdfSpreadsheetDocument.loadDocument(new ByteArrayInputStream(fileBytes))) {
            OdfTable sheet = doc.getTableByName("Regional Sales Performance");
            assertNotNull(sheet);

            OdfTableColumn dateColumn = sheet.getColumnByIndex(3);
            int dateLength = "Last Sale Date".length(); // Header is the longest string
            int expectedDateWidth = ColumnWidthHeuristic.calculateWidth(dateLength);
            long actualDateWidth = dateColumn.getWidth();
            String message = "Width for date (<=30 chars) should be calculated correctly with small padding. Expected ~"
                    + expectedDateWidth + " but was " + actualDateWidth;
            assertTrue(Math.abs(expectedDateWidth - actualDateWidth) <= 2, message);
        }
    }

    @Test
    public void testCsvGeneration() throws Exception {
        String json = loadResource("/test-payload.json");

        String csvOutput = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("text/csv")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asString();

        assertNotNull(csvOutput);

        String[] lines = csvOutput.split("\\R");
        assertEquals(12, lines.length, "Should be 12 lines: 1 header + 11 data rows");
        assertEquals("\"Region\",\"Units Sold\",\"Total Revenue\",\"Last Sale Date\"", lines[0]);
        assertEquals("\"North\",5430,123456.78,\"2025-09-30\"", lines[1]);
        assertEquals("\"Total\",46510,1059154.38,\"\"", lines[11]);
    }

    @Test
    public void testOdsGenerationWithMalformedData() throws Exception {
        String json = loadResource("/test-payload-malformed.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/vnd.oasis.opendocument.spreadsheet")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        assertNotNull(fileBytes);

        try (var doc = OdfSpreadsheetDocument.loadDocument(new ByteArrayInputStream(fileBytes))) {
            OdfTable sheet = doc.getTableByName("Malformed Data");
            assertNotNull(sheet);

            OdfTableCell cellB2 = sheet.getCellByPosition(1, 1);
            assertEquals("float", cellB2.getValueType());
            assertEquals(100.0, cellB2.getDoubleValue(), 0.001);

            OdfTableCell cellB3 = sheet.getCellByPosition(1, 2);
            assertEquals("string", cellB3.getValueType());
            assertEquals("Invalid", cellB3.getStringValue());

            OdfTableCell cellB4 = sheet.getCellByPosition(1, 3);
            assertTrue(cellB4.getStringValue().isEmpty());
        }
    }

    @Test
    public void testHtmlGeneration() throws IOException {
        String json = loadResource("/test-payload.json");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_HTML) // Request HTML output
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .header("Content-Type", containsString(MediaType.TEXT_HTML))
                .header("Content-Disposition", "attachment; filename=\"Q3 2025 Comprehensive Sales Report.html\"")
                .body(
                        containsString("<!DOCTYPE html>"),
                        containsString("<h1>Q3 2025 Comprehensive Sales Report</h1>"),
                        containsString("<h2>Regional Sales Performance</h2>"),
                        containsString("North"),
                        containsString("123456.78"));
    }

    @Test
    public void testHtmlGenerationWithCustomCss() throws IOException {
        String json = loadResource("/test-payload-custom-css.json");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_HTML)
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .body(
                        containsString("<h1>Custom CSS Report</h1>"),
                        containsString("color: green;"),
                        containsString("border: 5px dashed blue;"),
                        containsString("background-color: orange;"));
    }

    @Test
    public void testPdfGenerationWithCustomVersion() throws IOException {
        String json = loadResource("/test-payload-pdf-options.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/pdf")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .header("Content-Disposition", "attachment; filename=\"Custom PDF Report.pdf\"")
                .extract().asByteArray();

        assertNotNull(fileBytes);
        assertTrue(fileBytes.length > 0);

        try (PdfDocument pdfDoc = new PdfDocument(
                new PdfReader(new ByteArrayInputStream(fileBytes)))) {
            assertEquals("PDF-1.7", pdfDoc.getPdfVersion().toString(), "The PDF version should match the request.");
            StringBuilder text = new StringBuilder();
            int numberOfPages = pdfDoc.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; i++) {
                SimpleTextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
                String pageText = PdfTextExtractor
                        .getTextFromPage(pdfDoc.getPage(i), strategy);
                text.append(pageText);
            }
            String fullText = text.toString();
            assertNotNull(fullText);
            assertTrue(fullText.contains("Custom PDF Report"), "PDF should contain the document title.");
            assertTrue(fullText.contains("PDF Options Test Table"), "PDF should contain the table name.");
            assertTrue(fullText.contains("PDF Version Test"), "PDF should contain data from the first row.");
        }
    }

    @Test
    public void testPdfWithStructuralElements() throws IOException {
        String json = loadResource("/test-payload-structural.json");

        byte[] fileBytes = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept("application/pdf")
                .body(json)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        assertNotNull(fileBytes);
        assertTrue(fileBytes.length > 0);

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(fileBytes)))) {
            String text = PdfTextExtractor.getTextFromPage(pdfDoc.getFirstPage());

            assertNotNull(text);
            assertTrue(text.contains("Electronics"), "PDF should contain row header data.");
            assertTrue(text.contains("Total"), "PDF should contain the column footer row title.");
            assertTrue(text.contains("496000"), "PDF should contain the column footer data.");
        }
    }

    @Test
    public void testGenerationPerformance() throws IOException {
        Map<String, String> formats = new LinkedHashMap<>();
        formats.put("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        formats.put("ODS", "application/vnd.oasis.opendocument.spreadsheet");
        formats.put("CSV", "text/csv");
        formats.put("HTML", MediaType.TEXT_HTML);
        formats.put("PDF", "application/pdf");

        String payload10Rows = loadResource("/test-payload-10-rows.json");
        String payload100Rows = loadResource("/test-payload-100-rows.json");

        Map<String, Long> results10Rows = new LinkedHashMap<>();
        Map<String, Long> results100Rows = new LinkedHashMap<>();

        LOGGER.info("\n--- Starting Generation Performance Test ---");

        for (Map.Entry<String, String> format : formats.entrySet()) {
            long startTime10 = System.currentTimeMillis();
            given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(format.getValue())
                    .body(payload10Rows)
                    .when().post("/tables")
                    .then().statusCode(200);
            long endTime10 = System.currentTimeMillis();
            results10Rows.put(format.getKey(), endTime10 - startTime10);

            long startTime100 = System.currentTimeMillis();
            given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(format.getValue())
                    .body(payload100Rows)
                    .when().post("/tables")
                    .then().statusCode(200);
            long endTime100 = System.currentTimeMillis();
            results100Rows.put(format.getKey(), endTime100 - startTime100);
        }

        StringBuilder table = new StringBuilder();
        table.append("\n--- Generation Performance Results ---\n");
        table.append("--------------------------------------------------\n");
        table.append(String.format("| %-10s | %-15s | %-15s |\n", "Format", "10 Rows (ms)", "100 Rows (ms)"));
        table.append("--------------------------------------------------\n");
        for (String formatKey : formats.keySet()) {
            table.append(String.format("| %-10s | %-15d | %-15d |\n",
                    formatKey,
                    results10Rows.get(formatKey),
                    results100Rows.get(formatKey)));
        }
        table.append("--------------------------------------------------");
        LOGGER.info(table.toString());
    }

    private String loadResource(String path) throws IOException {
        try (InputStream is = TabloidResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(is, "Resource could not be found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}