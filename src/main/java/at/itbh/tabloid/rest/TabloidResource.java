package at.itbh.tabloid.rest;

import at.itbh.tabloid.model.TabloidRequest;
import at.itbh.tabloid.xlsx.XlsxGenerator;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

@Path("/tables")
public class TabloidResource {

    @Inject
    XlsxGenerator xlsxGenerator;

    public TabloidResource(XlsxGenerator xlsxGenerator) {
        this.xlsxGenerator = xlsxGenerator;
    }

    /**
     * Handles requests for XLSX file generation.
     *
     * @param request The tabloid request payload.
     * @return A JAX-RS Response containing the XLSX file.
     * @throws IOException if the file generation fails.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response createXlsx(TabloidRequest request) throws IOException {
        byte[] xlsx = xlsxGenerator.generate(request);
        String filename = request.document().title() + ".xlsx";
        return Response.ok(xlsx)
                .header("Content-Disposition", "attachment; filename=\"" + filename + ".xlsx\"")
                .build();
    }
}