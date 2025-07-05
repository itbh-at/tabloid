package at.itbh.tabloid.model.format;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CsvFormatOptions(
        @JsonProperty("charset") String charset,
        @JsonProperty("delimiter") String delimiter,
        @JsonProperty("alwaysQuoteEmptyStrings") Boolean alwaysQuoteEmptyStrings,
        @JsonProperty("alwaysQuoteStrings") Boolean alwaysQuoteStrings,
        @JsonProperty("escapeControlCharsWithEscapeChar") String escapeControlCharsWithEscapeChar,
        @JsonProperty("escapeQuoteCharWithEscapeChar") String escapeQuoteCharWithEscapeChar,
        @JsonProperty("omitMissingTailColumns") Boolean omitMissingTailColumns,
        @JsonProperty("includeHeader") Boolean includeHeader) implements FormatOptions {
}