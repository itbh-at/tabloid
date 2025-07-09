package at.itbh.tabloid.model.format;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HtmlFormatOptions(
        @JsonProperty("css") String css) implements FormatOptions {
}