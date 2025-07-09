package at.itbh.tabloid.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.itbh.tabloid.csv.CsvGenerator;
import at.itbh.tabloid.html.HtmlGenerator;
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
    private final CsvGenerator csvGenerator;
    private final HtmlGenerator htmlGenerator;
    private final ObjectMapper objectMapper;

    public TabloidResource(XlsxGenerator xlsxGenerator, OdsGenerator odsGenerator, CsvGenerator csvGenerator,
            HtmlGenerator htmlGenerator, ObjectMapper objectMapper) {
        this.xlsxGenerator = xlsxGenerator;
        this.odsGenerator = odsGenerator;
        this.csvGenerator = csvGenerator;
        this.htmlGenerator = htmlGenerator;
        this.objectMapper = objectMapper;
    }

    private Response processRequest(JsonNode requestNode, GeneratorFunction generatorFunction) throws Exception {
        JsonNode versionNode = requestNode.get("version");
        if (versionNode == null || !versionNode.isTextual()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Request body must include a 'version' field as a string.\"}").build();
        }

        String version = versionNode.asText();
        switch (version) {
            case "1.0":
                try {
                    TabloidRequest request = objectMapper.treeToValue(requestNode, TabloidRequest.class);
                    return generatorFunction.generate(request);
                } catch (JsonProcessingException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"Invalid JSON structure for version 1.0: " + e.getMessage() + "\"}")
                            .build();
                }
            default:
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Unsupported request version: " + version + "\"}").build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response createXlsx(JsonNode requestNode) throws Exception {
        return processRequest(requestNode, (request) -> {
            byte[] xlsx = xlsxGenerator.generate(request);
            String filename = request.document().title() + ".xlsx";
            return Response.ok(xlsx)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.oasis.opendocument.spreadsheet")
    public Response createOds(JsonNode requestNode) throws Exception {
        return processRequest(requestNode, (request) -> {
            byte[] ods = odsGenerator.generate(request);
            String filename = request.document().title() + ".ods";
            return Response.ok(ods)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("text/csv")
    public Response createCsv(JsonNode requestNode) throws Exception {
        return processRequest(requestNode, (request) -> {
            byte[] csv = csvGenerator.generate(request);
            String filename = request.document().title() + ".csv";
            return Response.ok(csv)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public Response createHtml(JsonNode requestNode) throws Exception {
        return processRequest(requestNode, (request) -> {
            String html = htmlGenerator.generate(request);
            String filename = request.document().title() + ".html";
            return Response.ok(html)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        });
    }

    @FunctionalInterface
    interface GeneratorFunction {
        Response generate(TabloidRequest request) throws Exception;
    }
}