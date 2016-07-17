package de.bischinger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.Response.ok;

@Path("/app")
@Produces(MediaType.TEXT_PLAIN)
@Api(value = "/time", description = "Get the time", tags = "time")
@ApplicationScoped
public class RegularResource {

    @PersistenceContext
    EntityManager em;

    @GET
    @ApiOperation(value = "Get the current time",
            notes = "Returns the time as a string",
            response = String.class
    )
    @Path("/time")
    public void time(@Suspended final AsyncResponse asyncResponse) {
        asyncResponse.resume(currentTimeMillis());
    }

    @GET
    @Path("/employees")
    public Response employees() {
        return ok(em.createNamedQuery("Employee.findAll", Employee.class)
                .getResultList().stream().map(Object::toString).collect(joining()))
                .build();
    }
}