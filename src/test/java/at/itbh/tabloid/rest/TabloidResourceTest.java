package at.itbh.tabloid.rest;

import at.itbh.tabloid.model.TabloidRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class TabloidResourceTest {

    @Test
    public void testFullJsonDeserialization() throws IOException {
        try (InputStream is = TabloidResourceTest.class.getResourceAsStream("/test-payload.json")) {
            assertNotNull(is, "test-payload.json not found");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            TabloidRequest result = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json)
                    .when().post("/table")
                    .then()
                    .statusCode(200)
                    .extract().as(TabloidRequest.class);

            assertEquals("test", result.document().title());
        }
    }
}