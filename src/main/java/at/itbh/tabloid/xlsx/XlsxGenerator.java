package at.itbh.tabloid.xlsx;

import at.itbh.tabloid.model.TabloidRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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

            request.tables().forEach(table -> {
                Sheet sheet = workbook.createSheet(table.name());
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < table.columns().size(); i++) {
                    headerRow.createCell(i).setCellValue(table.columns().get(i).name());
                }

                int rowNum = 1;
                for (List<Object> dataRow : table.rows()) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < dataRow.size(); i++) {
                        Cell cell = row.createCell(i);
                        Object cellValue = dataRow.get(i);
                        if (cellValue != null) {
                            cell.setCellValue(cellValue.toString());
                        }
                    }
                }
            });

            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}