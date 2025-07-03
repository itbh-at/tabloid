package at.itbh.tabloid;

import at.itbh.tabloid.model.TableRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/tables")
public class TableResource {

    private static final Logger LOG = Logger.getLogger(TableResource.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(TableRequest request) {
        LOG.infof("Received request for title: %s", request.document().title());
        LOG.infof("Number of tables: %d", request.tables().size());
        return Response.ok("Request processed successfully").build();
    }
}