package at.itbh.tabloid.model;

import at.itbh.tabloid.model.format.FormatOptions;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

public record TabloidRequest(
                @JsonProperty("version") String version,
                @JsonProperty("document") Document document,
                @JsonProperty("tables") List<Table> tables) {

        public <T extends FormatOptions> Optional<T> getFormatOptions(Class<T> formatType) {
                if (document == null || document.formats() == null) {
                        return Optional.empty();
                }
                return document.formats().stream()
                                .filter(formatType::isInstance)
                                .map(formatType::cast)
                                .findFirst();
        }
}