package at.itbh.tabloid.xlsx;

import at.itbh.tabloid.model.TabloidRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@ApplicationScoped
public class XlsxGenerator {

    /**
     * Generates an XLSX workbook based on the provided request data and returns it
     * as a byte array.
     *
     * @param request The complete tabloid request containing document metadata and
     *                table data.
     * @return A byte array representing the generated XLSX file.
     * @throws IOException If an error occurs during workbook writing.
     */
    public byte[] generate(TabloidRequest request) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}