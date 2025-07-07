package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TabloidRequest(
        @JsonProperty("version") String version,
        @JsonProperty("document") Document document,
        @JsonProperty("tables") List<Table> tables) {
}