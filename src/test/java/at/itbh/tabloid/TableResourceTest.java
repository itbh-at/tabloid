package at.itbh.tabloid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class TableResourceTest {

    @Test
    public void testFullJsonDeserialization() throws Exception {
        String requestBody = readTestResource("test-payload.json");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/tables")
                .then()
                .statusCode(200)
                .body(is("Request processed successfully"));
    }

    private String readTestResource(String fileName) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! " + fileName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}