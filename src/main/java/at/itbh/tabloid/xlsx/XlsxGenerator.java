package at.itbh.tabloid.xlsx;

import at.itbh.tabloid.config.XlsxConfig;
import at.itbh.tabloid.config.StyleConfig;
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

    private final XlsxConfig xlsxConfig;

    public XlsxGenerator(XlsxConfig xlsxConfig) {
        this.xlsxConfig = xlsxConfig;
    }

    public byte[] generate(TabloidRequest request) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            setDocumentProperties(request.document(), workbook);
            Map<String, CellStyle> styleCache = createStyles(workbook);

            final XlsxFormatOptions xlsxOptions = request.document().formats().stream()
                    .filter(XlsxFormatOptions.class::isInstance)
                    .map(XlsxFormatOptions.class::cast)
                    .findFirst()
                    .orElse(null);

            for (Table table : request.tables()) {
                Sheet sheet = workbook.createSheet(table.name());
                applyFreezePanes(sheet, table, xlsxOptions);
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < table.columns().size(); i++) {
                    Cell headerCell = headerRow.createCell(i);
                    headerCell.setCellValue(table.columns().get(i).name());
                    headerCell.setCellStyle(styleCache.get("header")); // Use named style
                }

                int rowNum = 1;
                for (List<Object> dataRow : table.rows()) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < dataRow.size(); i++) {
                        createCell(workbook, row, i, dataRow.get(i), table.columns().get(i), styleCache);
                    }
                }
                if (xlsxConfig.autoSizeColumns()) {
                    for (int i = 0; i < table.columns().size(); i++) {
                        sheet.autoSizeColumn(i);
                    }
                }
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

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

    private void createCell(XSSFWorkbook workbook, Row row, int colIndex, Object value, Column column,
            Map<String, CellStyle> styleCache) {
        Cell cell = row.createCell(colIndex);
        if (value == null) {
            cell.setCellStyle(styleCache.get("data"));
            return;
        }

        switch (column.type()) {
            case "number":
            case "currency":
                cell.setCellValue(Double.parseDouble(value.toString()));
                String numberFormat = column.format();
                if (numberFormat != null && !numberFormat.isBlank()) {
                    CellStyle numberStyle = styleCache.computeIfAbsent(numberFormat, k -> {
                        CellStyle newStyle = workbook.createCellStyle();
                        newStyle.cloneStyleFrom(styleCache.get("data"));
                        newStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(k));
                        return newStyle;
                    });
                    cell.setCellStyle(numberStyle);
                } else {
                    cell.setCellStyle(styleCache.get("data"));
                }
                break;
            case "date":
                cell.setCellValue(LocalDate.parse(value.toString()));
                String dateFormat = "yyyy-mm-dd";
                CellStyle dateStyle = styleCache.computeIfAbsent(dateFormat, k -> {
                    CellStyle newStyle = workbook.createCellStyle();
                    newStyle.cloneStyleFrom(styleCache.get("data"));
                    newStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(k));
                    return newStyle;
                });
                cell.setCellStyle(dateStyle);
                break;
            case "timestamp":
                cell.setCellValue(
                        Date.from(ZonedDateTime.parse(value.toString(), FLEXIBLE_TIMESTAMP_FORMATTER).toInstant()));
                String tsFormat = "yyyy-mm-dd hh:mm:ss";
                CellStyle tsStyle = styleCache.computeIfAbsent(tsFormat, k -> {
                    CellStyle newStyle = workbook.createCellStyle();
                    newStyle.cloneStyleFrom(styleCache.get("data"));
                    newStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(k));
                    return newStyle;
                });
                cell.setCellStyle(tsStyle);
                break;
            case "string":
            default:
                cell.setCellValue(value.toString());
                cell.setCellStyle(styleCache.get("data"));
                break;
        }
    }

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

    private Map<String, CellStyle> createStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();
        styles.put("header", createCellStyle(workbook, xlsxConfig.header()));
        styles.put("data", createCellStyle(workbook, xlsxConfig.data()));
        return styles;
    }

    private CellStyle createCellStyle(Workbook workbook, StyleConfig styleConfig) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        var fontConfig = styleConfig.font();
        font.setFontName(fontConfig.name());
        font.setFontHeightInPoints(fontConfig.size());
        font.setBold(fontConfig.bold());
        font.setItalic(fontConfig.italic());
        fontConfig.color().ifPresent(colorName -> {
            try {
                font.setColor(IndexedColors.valueOf(colorName.toUpperCase()).getIndex());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid font color name in config: " + colorName);
            }
        });
        style.setFont(font);
        styleConfig.backgroundColor().ifPresent(colorName -> {
            try {
                style.setFillForegroundColor(IndexedColors.valueOf(colorName.toUpperCase()).getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid background color name in config: " + colorName);
            }
        });

        return style;
    }
}