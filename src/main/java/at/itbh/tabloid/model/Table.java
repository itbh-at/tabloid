package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Table {
    @JsonProperty("name")
    public String name;

    @JsonProperty("columns")
    public List<Column> columns;

    @JsonProperty("rows")
    public List<List<Object>> rows;
}