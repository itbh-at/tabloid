package at.itbh.tabloid.model.format;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CsvFormatOptions.class, name = "csv"),
        @JsonSubTypes.Type(value = XlsxFormatOptions.class, name = "xlsx"),
        @JsonSubTypes.Type(value = PdfFormatOptions.class, name = "pdf")
})
public sealed interface FormatOptions permits CsvFormatOptions, PdfFormatOptions, XlsxFormatOptions {
}