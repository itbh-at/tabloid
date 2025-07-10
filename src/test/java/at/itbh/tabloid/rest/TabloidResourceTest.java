package at.itbh.tabloid.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableColumn;

import at.itbh.tabloid.util.ColumnWidthHeuristic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TabloidResourceTest {

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

            CellStyle dataStyle = dataRow.getCell(0).getCellStyle();
            Font dataFont = workbook.getFontAt(dataStyle.getFontIndex());
            assertFalse(dataFont.getBold(), "Data font should NOT be bold.");
            assertEquals("Arial", dataFont.getFontName(), "Data font should be Arial.");

            int longHeaderColumnIndex = 2;
            assertTrue(sheet1.getColumnWidth(longHeaderColumnIndex) > (12 * 256),
                    "Column with long header should be auto-sized.");
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
            assertEquals("Q3 2025 Sales Report", doc.getOfficeMetadata().getTitle());
            assertEquals("ITBH", doc.getOfficeMetadata().getCreator());

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

            // Test a column with content UNDER the threshold
            OdfTableColumn updatedAtColumn = sheet.getColumnByIndex(4);
            int updatedAtLength = "2025-10-01T11:05:00+0200".length();
            int expectedUpdatedAtWidth = ColumnWidthHeuristic.calculateWidth(updatedAtLength);
            long actualUpdatedAtWidth = updatedAtColumn.getWidth();
            String message = "Width for date (<=30 chars) should be calculated correctly with small padding. Expected ~"
                    + expectedUpdatedAtWidth + " but was " + actualUpdatedAtWidth;
            assertTrue(Math.abs(expectedUpdatedAtWidth - actualUpdatedAtWidth) <= 2, message);
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
        assertEquals(6, lines.length, "Should be 6 lines: 1 header + 5 data rows");
        assertEquals("\"Region\",\"Units Sold\",\"Total Revenue\",\"Last Sale Date\",\"Updated At\"", lines[0]);
        assertEquals("\"North\",5430,123456.78,\"2025-09-30\",\"2025-10-01T10:18:39Z\"", lines[1]);
        assertEquals("\"Central\",\"\",\"\",\"\",\"\"", lines[5]);
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
                .header("Content-Disposition", "attachment; filename=\"Q3 2025 Sales Report.html\"")
                .body(
                        containsString("<!DOCTYPE html>"),
                        containsString("<h1>Q3 2025 Sales Report</h1>"),
                        containsString("<h2>Regional Sales Performance</h2>"),
                        containsString("<td>North</td>"),
                        containsString("<td>123456.78</td>"));
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

    private String loadResource(String path) throws IOException {
        try (InputStream is = TabloidResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(is, "Resource could not be found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}