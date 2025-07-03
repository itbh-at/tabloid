package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Column(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("format") String format,
        @JsonProperty("symbol") String symbol) {
}