package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TableRequest {
    @JsonProperty("document")
    public Document document;

    @JsonProperty("tables")
    public List<Table> tables;
}