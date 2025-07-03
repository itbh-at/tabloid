package at.itbh.tabloid.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Document {
    @JsonProperty("title")
    public String title;

    @JsonProperty("author")
    public String author;

    @JsonProperty("subject")
    public String subject;

    @JsonProperty("keywords")
    public List<String> keywords;

    @JsonProperty("locale")
    public String locale;
}