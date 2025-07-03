package at.itbh.tabloid.model.format;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PdfFormatOptions(
        @JsonProperty("version") Double version,
        @JsonProperty("size") String size) implements FormatOptions {
}