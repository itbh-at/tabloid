package at.itbh.tabloid.ods;

import at.itbh.tabloid.common.ColumnType;
import at.itbh.tabloid.config.FontConfig;
import at.itbh.tabloid.config.OdsConfig;
import at.itbh.tabloid.config.StyleConfig;
import at.itbh.tabloid.model.Column;
import at.itbh.tabloid.model.Table;
import at.itbh.tabloid.model.TabloidRequest;
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

@ApplicationScoped
public class OdsGenerator {

    private static final String HEADER_STYLE_NAME = "HeaderStyle";
    private static final String DATA_STYLE_NAME = "DataStyle";

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

            createNamedStyles(doc);

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

                List<Integer> maxWidths = new ArrayList<>();
                for (int colIndex = 0; colIndex < tableData.columns().size(); colIndex++) {
                    maxWidths.add(findMaxColumnWidth(tableData, colIndex));
                }
                for (int colIndex = 0; colIndex < tableData.columns().size(); colIndex++) {
                    OdfTableColumn column = sheet.getColumnByIndex(colIndex);
                    long width = maxWidths.get(colIndex);
                    column.setWidth(width);
                }

                for (int colIndex = 0; colIndex < tableData.columns().size(); colIndex++) {
                    OdfTableCell cell = sheet.getCellByPosition(colIndex, 0);
                    cell.setStringValue(tableData.columns().get(colIndex).name());
                    cell.getOdfElement().setAttributeNS(OdfDocumentNamespace.TABLE.getUri(), "table:style-name",
                            HEADER_STYLE_NAME);
                }

                for (int rowIndex = 0; rowIndex < tableData.rows().size(); rowIndex++) {
                    var rowData = tableData.rows().get(rowIndex);
                    for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                        var column = tableData.columns().get(colIndex);
                        var value = rowData.get(colIndex);
                        OdfTableCell cell = sheet.getCellByPosition(colIndex, rowIndex + 1);

                        setCellValue(cell, value, column);
                        cell.getOdfElement().setAttributeNS(OdfDocumentNamespace.TABLE.getUri(), "table:style-name",
                                DATA_STYLE_NAME);
                    }
                }
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    private int findMaxColumnWidth(Table tableData, int colIndex) {
        String headerName = tableData.columns().get(colIndex).name();
        int maxWidth = ColumnWidthHeuristic.calculateWidth(headerName);
        for (List<Object> rowData : tableData.rows()) {
            if (colIndex < rowData.size()) {
                Object cellValue = rowData.get(colIndex);
                if (cellValue != null) {
                    int cellWidth = ColumnWidthHeuristic.calculateWidth(cellValue.toString());
                    if (cellWidth > maxWidth) {
                        maxWidth = cellWidth;
                    }
                }
            }
        }
        return maxWidth;
    }

    private void createNamedStyles(OdfSpreadsheetDocument doc) throws Exception {
        OdfOfficeAutomaticStyles styles = doc.getContentDom().getOrCreateAutomaticStyles();

        OdfStyle headerStyle = styles.newStyle(OdfStyleFamily.TableCell);
        headerStyle.setStyleNameAttribute(HEADER_STYLE_NAME);
        populateStyleProperties(headerStyle, odsConfig.header());

        OdfStyle dataStyle = styles.newStyle(OdfStyleFamily.TableCell);
        dataStyle.setStyleNameAttribute(DATA_STYLE_NAME);
        populateStyleProperties(dataStyle, odsConfig.data());
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