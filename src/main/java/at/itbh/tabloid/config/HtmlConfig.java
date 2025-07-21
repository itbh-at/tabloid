package at.itbh.tabloid.config;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;

@ConfigMapping(prefix = "tabloid.output.html")
public interface HtmlConfig {
    /**
     * An optional URI to an external CSS file for styling the HTML output.
     * e.g. classpath:default.css, file://..../default.css
     * 
     * @return An Optional containing the CSS URI string.
     */
    Optional<String> css();
}