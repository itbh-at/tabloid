package at.itbh.tabloid.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

            // The longest text overall is in the "Updated At" column:
            // "2025-10-01T11:05:00+0200" (25 chars)
            int expectedWidth = ColumnWidthHeuristic.calculateWidth("2025-10-01T11:05:00+0200");

            OdfTableColumn updatedAtColumn = sheet.getColumnByIndex(4);
            assertNotNull(updatedAtColumn);

            assertEquals(expectedWidth, updatedAtColumn.getWidth());
        }
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

    private String loadResource(String path) throws IOException {
        try (InputStream is = TabloidResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(is, "Resource could not be found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}