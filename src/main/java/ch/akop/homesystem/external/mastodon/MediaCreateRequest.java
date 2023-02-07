package ch.akop.homesystem.external.mastodon;

import lombok.Data;
import org.jboss.resteasy.reactive.PartFilename;
import org.jboss.resteasy.reactive.PartType;

import javax.ws.rs.FormParam;
import java.io.ByteArrayInputStream;


@Data
public class MediaCreateRequest {
    @FormParam("file")
    @PartFilename("image.jpg")
    @PartType("image/jpeg")
    public final ByteArrayInputStream file;
}
