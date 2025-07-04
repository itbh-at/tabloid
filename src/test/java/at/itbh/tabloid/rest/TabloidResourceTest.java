package at.itbh.tabloid.rest;

import at.itbh.tabloid.model.TabloidRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TabloidResourceTest {

    @Test
    public void testFullJsonDeserialization() throws IOException {
        try (InputStream is = TabloidResourceTest.class.getResourceAsStream("/test-payload.json")) {
            assertNotNull(is, "test-payload.json could not be found.");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            TabloidRequest result = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json)
                    .when().post("/table")
                    .then()
                    .statusCode(200)
                    .extract().as(TabloidRequest.class);

            assertEquals("Q3 2025 Sales Report", result.document().title());
        }
    }

    /**
     * Tests the full XLSX generation flow.
     * It sends a request and then inspects the returned .xlsx file to verify its
     * contents.
     */
    @Test
    public void testXlsxGeneration() throws IOException {
        try (InputStream is = TabloidResourceTest.class.getResourceAsStream("/test-payload.json")) {
            assertNotNull(is, "test-payload.json could not be found.");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            byte[] fileBytes = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(json)
                    .when().post("/table")
                    .then()
                    .statusCode(200)
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .extract().asByteArray();

            assertNotNull(fileBytes, "The returned file bytes should not be null.");
            assertTrue(fileBytes.length > 0, "The returned file should not be empty.");

            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
                assertEquals(1, workbook.getNumberOfSheets(), "Workbook should have one sheet.");

                Sheet sheet1 = workbook.getSheet("Regional Sales Performance");
                assertNotNull(sheet1, "Sheet 'Regional Sales Performance' should exist.");

                Row headerRow = sheet1.getRow(0);
                assertNotNull(headerRow, "Header row should exist.");
                assertEquals("Units Sold", headerRow.getCell(1).getStringCellValue(), "Header cell should match.");

                Row dataRow = sheet1.getRow(1);
                assertNotNull(dataRow, "First data row should exist.");
                assertEquals("North", dataRow.getCell(0).getStringCellValue(), "Data cell should match.");
            }
        }
    }
}