package at.itbh.tabloid.csv;

import at.itbh.tabloid.model.Column;
import at.itbh.tabloid.model.TabloidRequest;
import at.itbh.tabloid.model.format.CsvFormatOptions;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator.Feature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CsvGenerator {

    private final CsvConfig defaultConfig;
    private final CsvMapper csvMapper;

    public CsvGenerator(CsvConfig csvConfig) {
        this.defaultConfig = csvConfig;
        this.csvMapper = new CsvMapper();
    }

    /**
     * Generates a CSV file as a byte array from the given request.
     * <p>
     * Note: CSV generation only supports a single table. If the request contains
     * multiple tables, only the first one will be processed.
     *
     * @param request The TabloidRequest containing the data and format options.
     * @return A byte array representing the generated CSV file.
     * @throws Exception if an error occurs during CSV writing.
     */
    public byte[] generate(TabloidRequest request) throws Exception {
        if (request.tables() == null || request.tables().isEmpty()) {
            return new byte[0];
        }

        var table = request.tables().get(0);

        final CsvFormatOptions csvOptions = request.getFormatOptions(CsvFormatOptions.class)
                .orElse(new CsvFormatOptions(null, null, null, null, null, null, null, null, null));

        CsvMapper mapper = createConfiguredMapper(csvOptions);
        CsvSchema schema = createConfiguredSchema(csvOptions, table.columns());

        List<List<Object>> data = table.rows();

        boolean trim = Optional.ofNullable(csvOptions.trimValues()).orElse(defaultConfig.trimValues());
        if (trim) {
            data = table.rows().stream()
                    .map(row -> row.stream()
                            .map(val -> (val instanceof String) ? ((String) val).trim() : val)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
        }

        String csvOutput = mapper
                .writer(schema)
                .writeValueAsString(data);

        String charsetName = Optional.ofNullable(csvOptions.charset()).orElse(defaultConfig.charset());
        return csvOutput.getBytes(charsetName);
    }

    private CsvMapper createConfiguredMapper(CsvFormatOptions options) {
        CsvMapper mapper = new CsvMapper();

        boolean alwaysQuoteStrings = Optional.ofNullable(options.alwaysQuoteStrings())
                .orElse(defaultConfig.alwaysQuoteStrings());
        mapper.configure(Feature.ALWAYS_QUOTE_STRINGS, alwaysQuoteStrings);

        boolean omitMissingTail = Optional.ofNullable(options.omitMissingTailColumns())
                .orElse(defaultConfig.omitMissingTailColumns());
        mapper.configure(Feature.OMIT_MISSING_TAIL_COLUMNS, omitMissingTail);

        return mapper;
    }

    private CsvSchema createConfiguredSchema(CsvFormatOptions options, List<Column> columns) {
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        List<String> columnNames = columns.stream().map(Column::name).collect(Collectors.toList());
        for (String colName : columnNames) {
            schemaBuilder.addColumn(colName);
        }

        String delimiter = Optional.ofNullable(options.delimiter()).orElse(defaultConfig.delimiter());
        schemaBuilder.setColumnSeparator(delimiter.charAt(0));

        boolean useHeader = Optional.ofNullable(options.includeHeader()).orElse(defaultConfig.includeHeader());
        schemaBuilder.setUseHeader(useHeader);

        boolean quoteEmpty = Optional.ofNullable(options.alwaysQuoteEmptyStrings())
                .orElse(defaultConfig.alwaysQuoteEmptyStrings());
        if (quoteEmpty) {
            schemaBuilder.setNullValue("\"\"");
        }

        Optional<String> escapeChar = Optional.ofNullable(options.escapeQuoteCharWithEscapeChar())
                .or(() -> Optional.ofNullable(options.escapeControlCharsWithEscapeChar()));

        escapeChar.ifPresent(s -> schemaBuilder.setEscapeChar(s.charAt(0)));

        return schemaBuilder.build();
    }
}