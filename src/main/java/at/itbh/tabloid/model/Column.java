package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Column {
    @JsonProperty("name")
    public String name;

    @JsonProperty("type")
    public String type;

    @JsonProperty("format")
    public String format;

    @JsonProperty("symbol")
    public String symbol;
}