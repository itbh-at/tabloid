package at.itbh.tabloid.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.*;
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
    public void testXlsxGeneration() throws IOException {
        try (InputStream is = TabloidResourceTest.class.getResourceAsStream("/test-payload.json")) {
            assertNotNull(is, "test-payload.json could not be found.");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

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
                assertTrue(sheet1.getColumnWidth(longHeaderColumnIndex) > (8 * 256),
                        "Column with long header should be auto-sized.");
            }
        }
    }
}