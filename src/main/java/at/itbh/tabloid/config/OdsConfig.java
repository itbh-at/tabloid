package at.itbh.tabloid.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "tabloid.output.ods")
public interface OdsConfig {

    @WithDefault("true")
    boolean autoSizeColumns();

    StyleConfig header();

    StyleConfig data();
}