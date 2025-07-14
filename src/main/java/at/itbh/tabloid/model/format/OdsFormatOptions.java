package at.itbh.tabloid.model.format;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OdsFormatOptions(
        @JsonProperty("hasHeaderRow") Boolean hasHeaderRow,
        @JsonProperty("hasHeaderColumn") Boolean hasHeaderColumn,
        @JsonProperty("hasFooterColumn") Boolean hasFooterColumn,
        @JsonProperty("hasFooterRow") Boolean hasFooterRow) implements FormatOptions {
}