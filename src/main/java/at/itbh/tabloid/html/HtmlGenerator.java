package at.itbh.tabloid.html;

import at.itbh.tabloid.config.HtmlConfig;
import at.itbh.tabloid.model.TabloidRequest;
import at.itbh.tabloid.model.format.HtmlFormatOptions;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@ApplicationScoped
public class HtmlGenerator {

    @Inject
    Engine qute;

    private final HtmlConfig htmlConfig;
    private final Template documentTemplate;

    public HtmlGenerator(HtmlConfig htmlConfig, Engine qute) {
        this.htmlConfig = htmlConfig;
        this.qute = qute;
        this.documentTemplate = qute.getTemplate("HtmlGenerator/document");
    }

    public String generate(TabloidRequest request) throws IOException, URISyntaxException {
        return generate(request, null);
    }

    public String generate(TabloidRequest request, String pageSize) throws IOException, URISyntaxException {
        Optional<String> cssPathOpt = request.getFormatOptions(HtmlFormatOptions.class)
                .map(HtmlFormatOptions::css)
                .or(htmlConfig::css);

        String cssContent = null;
        if (cssPathOpt.isPresent()) {
            cssContent = loadCssContent(cssPathOpt.get());
        }
        return documentTemplate
                .data("request", request)
                .data("cssContent", cssContent)
                .data("pageSize", pageSize)
                .render();
    }

    private String loadCssContent(String cssPath) throws IOException, URISyntaxException {
        if (cssPath == null || cssPath.isBlank()) {
            return null;
        }
        URI uri = new URI(cssPath);
        if ("classpath".equals(uri.getScheme())) {
            try (var is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(uri.getSchemeSpecificPart())) {
                if (is == null) {
                    throw new IOException("Classpath resource not found: " + cssPath);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            return Files.readString(Paths.get(uri));
        }
    }
}
