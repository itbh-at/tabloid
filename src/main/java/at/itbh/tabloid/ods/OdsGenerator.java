package at.itbh.tabloid.ods;

import at.itbh.tabloid.config.OdsConfig;
import at.itbh.tabloid.model.TabloidRequest;
import jakarta.enterprise.context.ApplicationScoped;

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;

import java.io.ByteArrayOutputStream;
import java.util.List;

@ApplicationScoped
public class OdsGenerator {

    private final OdsConfig odsConfig;

    public OdsGenerator(OdsConfig odsConfig) {
        this.odsConfig = odsConfig;
    }

    public byte[] generate(TabloidRequest request) throws Exception {
        try (var doc = OdfSpreadsheetDocument.newSpreadsheetDocument();
                var out = new ByteArrayOutputStream()) {

            doc.getOfficeMetadata().setCreator(request.document().author());
            doc.getOfficeMetadata().setTitle(request.document().title());
            doc.getOfficeMetadata().setSubject(request.document().subject());

            for (int i = 0; i < request.tables().size(); i++) {
                var tableData = request.tables().get(i);
                OdfTable sheet;
                if (i == 0) {
                    List<OdfTable> tables = doc.getTableList(false);
                    if (tables.isEmpty()) {
                        throw new IllegalStateException("No default sheet found in the spreadsheet document.");
                    }
                    sheet = tables.get(0);
                    sheet.setTableName(tableData.name());
                } else {
                    sheet = OdfTable.newTable(doc);
                    sheet.setTableName(tableData.name());
                }

                for (int colIndex = 0; colIndex < tableData.columns().size(); colIndex++) {
                    sheet.getCellByPosition(colIndex, 0).setStringValue(tableData.columns().get(colIndex).name());
                }

                for (int rowIndex = 0; rowIndex < tableData.rows().size(); rowIndex++) {
                    var rowData = tableData.rows().get(rowIndex);
                    for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                        Object value = rowData.get(colIndex);
                        if (value != null) {
                            sheet.getCellByPosition(colIndex, rowIndex + 1).setStringValue(value.toString());
                        }
                    }
                }
            }

            doc.save(out);
            return out.toByteArray();
        }
    }
}