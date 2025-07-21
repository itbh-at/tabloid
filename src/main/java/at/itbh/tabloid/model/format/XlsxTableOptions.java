package at.itbh.tabloid.model.format;

import com.fasterxml.jackson.annotation.JsonProperty;

public record XlsxTableOptions(
        @JsonProperty("freezeHeaderRow") Boolean freezeHeaderRow,
        @JsonProperty("freezeHeaderColumn") Boolean freezeHeaderColumn,
        @JsonProperty("hasFooterRow") Boolean hasFooterRow) {
}