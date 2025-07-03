package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Table(
        @JsonProperty("name") String name,
        @JsonProperty("columns") List<Column> columns,
        @JsonProperty("rows") List<List<Object>> rows) {
}