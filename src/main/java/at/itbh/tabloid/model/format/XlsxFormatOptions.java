package at.itbh.tabloid.model.format;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record XlsxFormatOptions(
        @JsonProperty("freezeHeaderRow") Boolean freezeHeaderRow,
        @JsonProperty("freezeHeaderColumn") Boolean freezeHeaderColumn,
        @JsonProperty("hasHeaderColumn") Boolean hasHeaderColumn,
        @JsonProperty("hasFooterColumn") Boolean hasFooterColumn,
        @JsonProperty("hasFooterRow") Boolean hasFooterRow,
        @JsonProperty("tables") Map<String, XlsxTableOptions> tables) implements FormatOptions {
}