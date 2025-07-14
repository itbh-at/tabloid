package at.itbh.tabloid.xlsx;

import at.itbh.tabloid.config.XlsxConfig;
import at.itbh.tabloid.common.ColumnType;
import at.itbh.tabloid.common.StyleKey;
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
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public byte[] generate(TabloidRequest request) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            setDocumentProperties(request.document(), workbook);
            Map<String, CellStyle> styleCache = createStyles(workbook);

            final Optional<XlsxFormatOptions> xlsxOptions = request.getFormatOptions(XlsxFormatOptions.class);
            boolean hasHeaderColumn = xlsxOptions.flatMap(o -> Optional.ofNullable(o.hasHeaderColumn())).orElse(false);
            boolean hasFooterRow = xlsxOptions.flatMap(o -> Optional.ofNullable(o.hasFooterRow())).orElse(false);

            for (Table table : request.tables()) {
                Sheet sheet = workbook.createSheet(table.name());
                applyFreezePanes(sheet, table, xlsxOptions.orElse(null));
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < table.columns().size(); i++) {
                    Cell headerCell = headerRow.createCell(i);
                    headerCell.setCellValue(table.columns().get(i).name());
                    headerCell.setCellStyle(styleCache.get(StyleKey.HEADER));
                }

                int numRows = table.rows().size();
                for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                    Row row = sheet.createRow(rowIndex + 1);
                    List<Object> dataRow = table.rows().get(rowIndex);
                    boolean isFooterRow = hasFooterRow && (rowIndex == numRows - 1);

                    for (int i = 0; i < dataRow.size(); i++) {
                        String styleKey;
                        if (isFooterRow) {
                            styleKey = StyleKey.FOOTER;
                        } else if (hasHeaderColumn && i == 0) {
                            styleKey = StyleKey.ROW_HEADER;
                        } else {
                            styleKey = StyleKey.DATA;
                        }
                        createCell(workbook, row, i, dataRow.get(i), table.columns().get(i), styleCache, styleKey);
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

        boolean freezeRow = Optional.ofNullable(globalOptions.freezeHeaderRow()).orElse(false);
        boolean freezeCol = Optional.ofNullable(globalOptions.freezeHeaderColumn()).orElse(false);

        if (globalOptions.tables() != null) {
            XlsxTableOptions tableOptions = globalOptions.tables().get(table.name());
            if (tableOptions != null) {
                freezeRow = Optional.ofNullable(tableOptions.freezeHeaderRow()).orElse(freezeRow);
                freezeCol = Optional.ofNullable(tableOptions.freezeHeaderColumn()).orElse(freezeCol);
            }
        }

        int colSplit = freezeCol ? 1 : 0;
        int rowSplit = freezeRow ? 1 : 0;
        if (colSplit > 0 || rowSplit > 0) {
            sheet.createFreezePane(colSplit, rowSplit);
        }
    }

    private void createCell(XSSFWorkbook workbook, Row row, int colIndex, Object value, Column column,
            Map<String, CellStyle> styleCache, String styleKey) {
        Cell cell = row.createCell(colIndex);
        if (value == null) {
            cell.setCellStyle(styleCache.get(styleKey));
            return;
        }

        switch (column.type()) {
            case ColumnType.NUMBER:
            case ColumnType.CURRENCY:
                try {
                    cell.setCellValue(Double.parseDouble(value.toString()));
                } catch (NumberFormatException e) {
                    cell.setCellValue(value.toString());
                }
                String numberFormat = column.format();
                if (numberFormat != null && !numberFormat.isBlank()) {
                    String cacheKey = styleKey + "_" + numberFormat;
                    CellStyle numberStyle = styleCache.computeIfAbsent(cacheKey, k -> {
                        CellStyle newStyle = workbook.createCellStyle();
                        newStyle.cloneStyleFrom(styleCache.get(styleKey));
                        newStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(numberFormat));
                        return newStyle;
                    });
                    cell.setCellStyle(numberStyle);
                } else {
                    cell.setCellStyle(styleCache.get(styleKey));
                }
                break;
            case ColumnType.DATE:
                if (value != null && !value.toString().isBlank()) {
                    cell.setCellValue(LocalDate.parse(value.toString()));
                }
                String dateFormat = "yyyy-mm-dd";
                String dateCacheKey = styleKey + "_" + dateFormat;
                CellStyle dateStyle = styleCache.computeIfAbsent(dateCacheKey, k -> {
                    CellStyle newStyle = workbook.createCellStyle();
                    newStyle.cloneStyleFrom(styleCache.get(styleKey));
                    newStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(dateFormat));
                    return newStyle;
                });
                cell.setCellStyle(dateStyle);
                break;
            case ColumnType.TIMESTAMP:
                if (value != null && !value.toString().isBlank()) {
                    cell.setCellValue(
                            Date.from(ZonedDateTime.parse(value.toString(), FLEXIBLE_TIMESTAMP_FORMATTER).toInstant()));
                }
                String tsFormat = "yyyy-mm-dd hh:mm:ss";
                String tsCacheKey = styleKey + "_" + tsFormat;
                CellStyle tsStyle = styleCache.computeIfAbsent(tsCacheKey, k -> {
                    CellStyle newStyle = workbook.createCellStyle();
                    newStyle.cloneStyleFrom(styleCache.get(styleKey));
                    newStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(tsFormat));
                    return newStyle;
                });
                cell.setCellStyle(tsStyle);
                break;
            case ColumnType.STRING:
            default:
                cell.setCellValue(value.toString());
                cell.setCellStyle(styleCache.get(styleKey));
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
        styles.put(StyleKey.HEADER, createCellStyle(workbook, xlsxConfig.header()));
        styles.put(StyleKey.DATA, createCellStyle(workbook, xlsxConfig.data()));
        styles.put(StyleKey.ROW_HEADER, createCellStyle(workbook, xlsxConfig.rowHeader()));
        styles.put(StyleKey.FOOTER, createCellStyle(workbook, xlsxConfig.footer()));
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