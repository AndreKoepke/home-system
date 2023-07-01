package ch.akop.homesystem.telemetry.exceptions


import io.quarkus.logging.LoggingFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Provider
class TooManyRequestsHandler : ExceptionMapper<TooManyRequests> {

    private val logger: Logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun toResponse(exception: TooManyRequests): Response {
        logger.warn("Too many requests from ${exception.who}")
        return Response.status(Response.Status.TOO_MANY_REQUESTS).build()
    }
}
