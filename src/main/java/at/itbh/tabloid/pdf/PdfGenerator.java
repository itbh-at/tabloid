package at.itbh.tabloid.pdf;

import at.itbh.tabloid.html.HtmlGenerator;
import at.itbh.tabloid.model.TabloidRequest;
import at.itbh.tabloid.model.format.HtmlFormatOptions;
import at.itbh.tabloid.model.format.PdfFormatOptions;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

@ApplicationScoped
public class PdfGenerator {

    private final HtmlGenerator htmlGenerator;

    public PdfGenerator(HtmlGenerator htmlGenerator) {
        this.htmlGenerator = htmlGenerator;
    }

    public byte[] generate(TabloidRequest request) throws Exception {
        Optional<PdfFormatOptions> pdfOptions = request.getFormatOptions(PdfFormatOptions.class);
        Optional<HtmlFormatOptions> htmlOptions = request.getFormatOptions(HtmlFormatOptions.class);
        String pageSize = pdfOptions.flatMap(o -> Optional.ofNullable(o.size())).orElse(null);
        Optional<Double> pdfVersion = pdfOptions.flatMap(o -> Optional.ofNullable(o.version()));

        boolean hasHeaderColumn = pdfOptions.flatMap(o -> Optional.ofNullable(o.hasHeaderColumn()))
                .orElse(htmlOptions.flatMap(h -> Optional.ofNullable(h.hasHeaderColumn())).orElse(false));

        boolean hasFooterRow = pdfOptions.flatMap(o -> Optional.ofNullable(o.hasFooterRow()))
                .orElse(htmlOptions.flatMap(h -> Optional.ofNullable(h.hasFooterRow())).orElse(false));

        String html = htmlGenerator.generate(request, pageSize, hasHeaderColumn, hasFooterRow);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.usePdfUaAccessibility(true);
            pdfVersion.ifPresent(v -> builder.usePdfVersion(v.floatValue()));
            builder.withProducer("tabloid - Your Table Droid <https://github.com/itbh-at/tabloid/>");
            builder.withHtmlContent(html, "classpath:/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }
}