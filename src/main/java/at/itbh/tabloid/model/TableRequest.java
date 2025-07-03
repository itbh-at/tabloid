package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TableRequest(
        @JsonProperty("document") Document document,
        @JsonProperty("tables") List<Table> tables) {
}