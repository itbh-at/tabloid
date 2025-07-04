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

@Path("/")
public class TabloidResource {

    @Inject
    XlsxGenerator xlsxGenerator;

    @POST
    @Path("/table")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TabloidRequest unmarshal(TabloidRequest tabloidRequest) {
        return tabloidRequest;
    }

    /**
     * Handles requests for XLSX file generation.
     *
     * @param request The tabloid request payload.
     * @return A JAX-RS Response containing the XLSX file.
     * @throws IOException if the file generation fails.
     */
    @POST
    @Path("/table")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response generateXlsx(TabloidRequest request) throws IOException {
        byte[] xlsx = xlsxGenerator.generate(request);

        return Response.ok(xlsx)
                .header("Content-Disposition", "attachment; filename=\"" + request.document().title() + ".xlsx\"")
                .build();
    }
}