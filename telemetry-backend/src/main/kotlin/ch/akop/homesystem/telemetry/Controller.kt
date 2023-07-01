package ch.akop.homesystem.telemetry

import ch.akop.homesystem.telemetry.models.Heartbeat
import io.vertx.ext.web.RoutingContext
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces

@Path("/telemetry")
class Controller(val service: Service,
                 val context: RoutingContext) {

    @POST
    @Path("sync")
    fun sync() = service.sync(context.request().remoteAddress().host())

    @POST
    @Path("heartbeat")
    fun heartbeat(heartbeat: Heartbeat) = service.heartBeat(heartbeat)

    @GET
    @Path("badge")
    @Produces("image/svg+xml")
    fun badge() = service.badge()

}
