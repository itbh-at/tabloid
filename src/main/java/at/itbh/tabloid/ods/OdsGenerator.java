package at.itbh.tabloid.ods;

import at.itbh.tabloid.config.OdsConfig;
import at.itbh.tabloid.model.TabloidRequest;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;

@ApplicationScoped
public class OdsGenerator {

    private final OdsConfig odsConfig;

    public OdsGenerator(OdsConfig odsConfig) {
        this.odsConfig = odsConfig;
    }

    public byte[] generate(TabloidRequest request) throws IOException {
        System.out.println("ODS Generator called for title: " + request.document().title());
        return new byte[0];
    }
}