package at.itbh.tabloid.rest;

import at.itbh.tabloid.model.TabloidRequest;
import at.itbh.tabloid.ods.OdsGenerator;
import at.itbh.tabloid.xlsx.XlsxGenerator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/tables")
public class TabloidResource {

    private final XlsxGenerator xlsxGenerator;
    private final OdsGenerator odsGenerator;

    public TabloidResource(XlsxGenerator xlsxGenerator, OdsGenerator odsGenerator) {
        this.xlsxGenerator = xlsxGenerator;
        this.odsGenerator = odsGenerator;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response createXlsx(TabloidRequest request) throws Exception {
        byte[] xlsx = xlsxGenerator.generate(request);
        String filename = request.document().title() + ".xlsx";
        return Response.ok(xlsx)
                .header("Content-Disposition", "attachment; filename=\"" + filename + ".xlsx\"")
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.oasis.opendocument.spreadsheet")
    public Response createOds(TabloidRequest request) throws Exception {
        byte[] ods = odsGenerator.generate(request);
        String filename = request.document().title() + ".ods";
        return Response.ok(ods)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }
}