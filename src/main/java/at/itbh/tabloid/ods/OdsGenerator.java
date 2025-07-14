package at.itbh.tabloid.ods;

import at.itbh.tabloid.common.ColumnType;
import at.itbh.tabloid.config.FontConfig;
import at.itbh.tabloid.config.OdsConfig;
import at.itbh.tabloid.config.StyleConfig;
import at.itbh.tabloid.model.Column;
import at.itbh.tabloid.model.Table;
import at.itbh.tabloid.model.TabloidRequest;
import at.itbh.tabloid.model.format.OdsFormatOptions;
import at.itbh.tabloid.util.ColumnWidthHeuristic;
import jakarta.enterprise.context.ApplicationScoped;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableColumn;
import org.odftoolkit.odfdom.dom.OdfDocumentNamespace;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.dom.style.props.OdfTableCellProperties;
import org.odftoolkit.odfdom.dom.style.props.OdfParagraphProperties;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeAutomaticStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OdsGenerator {

    private static final String HEADER_STYLE_NAME = "HeaderStyle";
    private static final String DATA_STYLE_NAME = "DataStyle";
    private static final String ROW_HEADER_STYLE_NAME = "RowHeaderStyle";
    private static final String FOOTER_STYLE_NAME = "FooterStyle";

    private final OdsConfig odsConfig;
    private static final DateTimeFormatter FLEXIBLE_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendOffsetId().optionalEnd()
            .optionalStart().appendOffset("+HHMM", "Z").optionalEnd()
            .toFormatter();

    public OdsGenerator(OdsConfig odsConfig) {
        this.odsConfig = odsConfig;
    }

    public byte[] generate(TabloidRequest request) throws Exception {
        try (var doc = OdfSpreadsheetDocument.newSpreadsheetDocument();
                var out = new ByteArrayOutputStream()) {

            doc.getOfficeMetadata().setCreator(request.document().author());
            doc.getOfficeMetadata().setTitle(request.document().title());
            doc.getOfficeMetadata().setSubject(request.document().subject());

            OdfOfficeAutomaticStyles styles = doc.getContentDom().getOrCreateAutomaticStyles();
            createNamedStyles(styles);

            Optional<OdsFormatOptions> formatOptions = request.getFormatOptions(OdsFormatOptions.class);
            boolean hasHeaderColumn = formatOptions.flatMap(o -> Optional.ofNullable(o.hasHeaderColumn()))
                    .orElse(false);
            boolean hasFooterRow = formatOptions.flatMap(o -> Optional.ofNullable(o.hasFooterRow())).orElse(false);

            for (int i = 0; i < request.tables().size(); i++) {
                var tableData = request.tables().get(i);
                OdfTable sheet;
                if (i == 0) {
                    List<OdfTable> tables = doc.getTableList(false);
                    sheet = tables.get(0);
                    sheet.setTableName(tableData.name());
                } else {
                    sheet = OdfTable.newTable(doc);
                    sheet.setTableName(tableData.name());
                }

                List<Integer> columnWidths = calculateColumnWidths(tableData);
                for (int colIndex = 0; colIndex < columnWidths.size(); colIndex++) {
                    OdfTableColumn column = sheet.getColumnByIndex(colIndex);
                    long width = columnWidths.get(colIndex);
                    column.setWidth(width);
                }

                for (int colIndex = 0; colIndex < tableData.columns().size(); colIndex++) {
                    OdfTableCell cell = sheet.getCellByPosition(colIndex, 0);
                    cell.setStringValue(tableData.columns().get(colIndex).name());
                    cell.getOdfElement().setAttributeNS(OdfDocumentNamespace.TABLE.getUri(), "table:style-name",
                            HEADER_STYLE_NAME);
                }

                int numRows = tableData.rows().size();
                for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                    var rowData = tableData.rows().get(rowIndex);
                    boolean isFooterRow = hasFooterRow && (rowIndex == numRows - 1);

                    for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                        var column = tableData.columns().get(colIndex);
                        var value = rowData.get(colIndex);
                        OdfTableCell cell = sheet.getCellByPosition(colIndex, rowIndex + 1);

                        setCellValue(cell, value, column);

                        String baseStyleName;
                        if (isFooterRow) {
                            baseStyleName = FOOTER_STYLE_NAME;
                        } else if (hasHeaderColumn && colIndex == 0) {
                            baseStyleName = ROW_HEADER_STYLE_NAME;
                        } else {
                            baseStyleName = DATA_STYLE_NAME;
                        }
                        String horizontalAlign;
                        if (column.alignment() != null && column.alignment().horizontal() != null) {
                            horizontalAlign = column.alignment().horizontal();
                        } else {
                            horizontalAlign = ColumnType.STRING.equals(column.type()) ? "left" : "right";
                        }
                        String verticalAlign = (column.alignment() != null && column.alignment().vertical() != null)
                                ? column.alignment().vertical()
                                : "top";
                        String finalStyleName = baseStyleName + "_" + horizontalAlign + "_" + verticalAlign;
                        if (styles.getStyle(finalStyleName, OdfStyleFamily.TableCell) == null) {
                            OdfStyle newStyle = styles.newStyle(OdfStyleFamily.TableCell);
                            newStyle.setStyleNameAttribute(finalStyleName);
                            newStyle.setStyleParentStyleNameAttribute(baseStyleName);
                            populateAlignment(newStyle, horizontalAlign, verticalAlign);
                        }
                        cell.getOdfElement().setAttributeNS(OdfDocumentNamespace.TABLE.getUri(), "table:style-name",
                                finalStyleName);
                    }
                }
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    private List<Integer> calculateColumnWidths(Table tableData) {
        List<Column> columns = tableData.columns();
        int numColumns = columns.size();
        int[] maxLengths = new int[numColumns];
        for (int i = 0; i < numColumns; i++) {
            String headerName = columns.get(i).name();
            maxLengths[i] = (headerName != null) ? headerName.length() : 0;
        }
        for (List<Object> rowData : tableData.rows()) {
            for (int i = 0; i < rowData.size() && i < numColumns; i++) {
                Object cellValue = rowData.get(i);
                if (cellValue != null) {
                    int currentLength = cellValue.toString().length();
                    if (currentLength > maxLengths[i]) {
                        maxLengths[i] = currentLength;
                    }
                }
            }
        }
        List<Integer> columnWidths = new ArrayList<>();
        for (int maxLength : maxLengths) {
            columnWidths.add(ColumnWidthHeuristic.calculateWidth(maxLength));
        }
        return columnWidths;
    }

    private void createNamedStyles(OdfOfficeAutomaticStyles styles) {
        OdfStyle headerStyle = styles.newStyle(OdfStyleFamily.TableCell);
        headerStyle.setStyleNameAttribute(HEADER_STYLE_NAME);
        populateStyleProperties(headerStyle, odsConfig.header());

        OdfStyle dataStyle = styles.newStyle(OdfStyleFamily.TableCell);
        dataStyle.setStyleNameAttribute(DATA_STYLE_NAME);
        populateStyleProperties(dataStyle, odsConfig.data());

        OdfStyle rowHeaderStyle = styles.newStyle(OdfStyleFamily.TableCell);
        rowHeaderStyle.setStyleNameAttribute(ROW_HEADER_STYLE_NAME);
        populateStyleProperties(rowHeaderStyle, odsConfig.rowHeader());

        OdfStyle footerStyle = styles.newStyle(OdfStyleFamily.TableCell);
        footerStyle.setStyleNameAttribute(FOOTER_STYLE_NAME);
        populateStyleProperties(footerStyle, odsConfig.footer());
    }

    private void populateAlignment(OdfStyle style, String horizontal, String vertical) {
        String horizontalValue = horizontal;
        if ("left".equalsIgnoreCase(horizontalValue)) {
            horizontalValue = "start";
        } else if ("right".equalsIgnoreCase(horizontalValue)) {
            horizontalValue = "end";
        }
        style.setProperty(OdfParagraphProperties.TextAlign, horizontalValue);
        style.setProperty(OdfTableCellProperties.VerticalAlign, vertical);
    }

    private void populateStyleProperties(OdfStyle style, StyleConfig styleConfig) {
        FontConfig fontConfig = styleConfig.font();
        style.setProperty(StyleTextPropertiesElement.FontName, fontConfig.name());
        style.setProperty(StyleTextPropertiesElement.FontSize, fontConfig.size() + "pt");
        style.setProperty(StyleTextPropertiesElement.FontWeight, fontConfig.bold() ? "bold" : "normal");
        style.setProperty(StyleTextPropertiesElement.FontStyle, fontConfig.italic() ? "italic" : "normal");
        fontConfig.color()
                .ifPresent(color -> style.setProperty(StyleTextPropertiesElement.Color, mapColorToHex(color)));

        styleConfig.backgroundColor()
                .ifPresent(color -> style.setProperty(OdfTableCellProperties.BackgroundColor, mapColorToHex(color)));
    }

    private void setCellValue(OdfTableCell cell, Object value, Column column) {
        if (value == null) {
            cell.setStringValue("");
            return;
        }

        switch (column.type()) {
            case ColumnType.NUMBER:
            case ColumnType.CURRENCY:
                try {
                    cell.setDoubleValue(Double.parseDouble(value.toString()));
                } catch (NumberFormatException e) {
                    cell.setStringValue(value.toString());
                }
                break;
            case ColumnType.DATE:
                try {
                    LocalDate date = LocalDate.parse(value.toString());
                    Calendar calendar = new GregorianCalendar(date.getYear(), date.getMonthValue() - 1,
                            date.getDayOfMonth());
                    cell.setDateValue(calendar);
                } catch (Exception e) {
                    cell.setStringValue(value.toString());
                }
                break;
            case ColumnType.TIMESTAMP:
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(value.toString(), FLEXIBLE_TIMESTAMP_FORMATTER);
                    Calendar tsCalendar = GregorianCalendar.from(zdt);
                    cell.setDateValue(tsCalendar);
                } catch (Exception e) {
                    cell.setStringValue(value.toString());
                }
                break;
            case ColumnType.STRING:
            default:
                cell.setStringValue(value.toString());
                break;
        }
    }

    private String mapColorToHex(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return "#FFFFFF";
        }
        return switch (colorName.toUpperCase()) {
            case "BLACK" -> "#000000";
            case "WHITE" -> "#FFFFFF";
            case "RED" -> "#FF0000";
            case "BLUE" -> "#0000FF";
            case "GREEN" -> "#008000";
            case "YELLOW" -> "#FFFF00";
            case "GREY_25_PERCENT" -> "#C0C0C0";
            default -> colorName;
        };
    }
}