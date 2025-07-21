package at.itbh.tabloid.config;

import io.smallrye.config.WithDefault;
import java.util.Optional;

public interface FontConfig {
    @WithDefault("Calibri")
    String name();

    @WithDefault("11")
    short size();

    @WithDefault("false")
    boolean bold();

    @WithDefault("false")
    boolean italic();

    Optional<String> color();
}