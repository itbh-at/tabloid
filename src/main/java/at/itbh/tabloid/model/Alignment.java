package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Alignment(
        @JsonProperty("horizontal") String horizontal,
        @JsonProperty("vertical") String vertical) {
}