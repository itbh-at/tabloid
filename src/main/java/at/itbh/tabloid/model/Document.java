package at.itbh.tabloid.model;

import at.itbh.tabloid.model.format.FormatOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Document(
        @JsonProperty("title") String title,
        @JsonProperty("author") String author,
        @JsonProperty("subject") String subject,
        @JsonProperty("keywords") List<String> keywords,
        @JsonProperty("locale") String locale,
        @JsonProperty("formats") List<FormatOptions> formats) {
}