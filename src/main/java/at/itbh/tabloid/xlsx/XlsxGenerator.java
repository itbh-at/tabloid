package at.itbh.tabloid.xlsx;

import at.itbh.tabloid.model.Column;
import at.itbh.tabloid.model.Document;
import at.itbh.tabloid.model.Table;
import at.itbh.tabloid.model.TabloidRequest;
import at.itbh.tabloid.model.format.XlsxFormatOptions;
import at.itbh.tabloid.model.format.XlsxTableOptions;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class XlsxGenerator {

    private static final String GENERATOR_NAME = "tabloid - Your Table Droid <https://github.com/itbh-at/tabloid/>";

    private static final DateTimeFormatter FLEXIBLE_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendOffsetId().optionalEnd()
            .optionalStart().appendOffset("+HHMM", "Z").optionalEnd()
            .toFormatter();

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

            setDocumentProperties(request.document(), workbook);
            Map<String, CellStyle> styleCache = new HashMap<>();

            final XlsxFormatOptions xlsxOptions = request.document().formats().stream()
                    .filter(XlsxFormatOptions.class::isInstance)
                    .map(XlsxFormatOptions.class::cast)
                    .findFirst()
                    .orElse(null);

            request.tables().forEach(table -> {
                Sheet sheet = workbook.createSheet(table.name());
                applyFreezePanes(sheet, table, xlsxOptions);
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < table.columns().size(); i++) {
                    headerRow.createCell(i).setCellValue(table.columns().get(i).name());
                }
                int rowNum = 1;
                for (List<Object> dataRow : table.rows()) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < dataRow.size(); i++) {
                        createCell(workbook, row, i, dataRow.get(i), table.columns().get(i), styleCache);
                    }
                }
            });

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * Applies freeze panes to a sheet based on global and table-specific options.
     */
    private void applyFreezePanes(Sheet sheet, Table table, XlsxFormatOptions globalOptions) {
        if (globalOptions == null) {
            return;
        }
        boolean freezeRow = globalOptions.freezeHeaderRow();
        boolean freezeCol = globalOptions.freezeHeaderColumn();
        XlsxTableOptions tableOptions = globalOptions.tables().get(table.name());
        if (tableOptions != null) {
            freezeRow = tableOptions.freezeHeaderRow();
            freezeCol = tableOptions.freezeHeaderColumn();
        }
        int colSplit = freezeCol ? 1 : 0;
        int rowSplit = freezeRow ? 1 : 0;
        if (colSplit > 0 || rowSplit > 0) {
            sheet.createFreezePane(colSplit, rowSplit);
        }
    }

    /**
     * Creates a cell and populates it based on the column's data type.
     */
    private void createCell(XSSFWorkbook workbook, Row row, int colIndex, Object value, Column column,
            Map<String, CellStyle> styleCache) {
        Cell cell = row.createCell(colIndex);
        if (value == null) {
            return;
        }

        switch (column.type()) {
            case "number":
            case "currency":
                cell.setCellValue(Double.parseDouble(value.toString()));
                String numberFormat = column.format();
                if (numberFormat != null && !numberFormat.isBlank()) {
                    CellStyle numberStyle = styleCache.get(numberFormat);
                    if (numberStyle == null) {
                        numberStyle = workbook.createCellStyle();
                        numberStyle
                                .setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(numberFormat));
                        styleCache.put(numberFormat, numberStyle);
                    }
                    cell.setCellStyle(numberStyle);
                }
                break;
            case "date":
                cell.setCellValue(LocalDate.parse(value.toString()));
                String dateFormat = "yyyy-mm-dd";
                CellStyle dateStyle = styleCache.get(dateFormat);
                if (dateStyle == null) {
                    dateStyle = workbook.createCellStyle();
                    dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(dateFormat));
                    styleCache.put(dateFormat, dateStyle);
                }
                cell.setCellStyle(dateStyle);
                break;
            case "timestamp":
                cell.setCellValue(
                        Date.from(ZonedDateTime.parse(value.toString(), FLEXIBLE_TIMESTAMP_FORMATTER).toInstant()));
                String tsFormat = "yyyy-mm-dd hh:mm:ss";
                CellStyle tsStyle = styleCache.get(tsFormat);
                if (tsStyle == null) {
                    tsStyle = workbook.createCellStyle();
                    tsStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(tsFormat));
                    styleCache.put(tsFormat, tsStyle);
                }
                cell.setCellStyle(tsStyle);
                break;
            case "string":
            default:
                cell.setCellValue(value.toString());
                break;
        }
    }

    /**
     * Sets the document's metadata (properties) on the generated workbook.
     * 
     * @param document The document metadata from the request.
     * @param workbook The workbook to apply properties to.
     */
    private void setDocumentProperties(Document document, XSSFWorkbook workbook) {
        POIXMLProperties.CoreProperties coreProps = workbook.getProperties().getCoreProperties();
        coreProps.setTitle(document.title());
        coreProps.setSubjectProperty(document.subject());
        coreProps.setCreator(document.author());
        List<String> keywords = document.keywords();
        if (keywords != null && !keywords.isEmpty()) {
            coreProps.setKeywords(String.join(", ", keywords));
        }
        POIXMLProperties.ExtendedProperties extProps = workbook.getProperties().getExtendedProperties();
        extProps.getUnderlyingProperties().setApplication(GENERATOR_NAME);
    }
}