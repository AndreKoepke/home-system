package ch.akop.homesystem.openai;

import ch.akop.homesystem.config.properties.MastodonProperties;
import ch.akop.homesystem.external.mastodon.MastodonService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class MastodonTest {


    @Test
    void test() throws IOException {
        var service = new MastodonService(new MastodonProperties("mastodon.akop.online", "z2rC98ztGGAM2fBpC14xRGENwV0irOHgybv4om44zyo"));
        service.initializeWebClients();

        service.publishImage("test", Files.readAllBytes(Path.of("test.jpg")));
        //service.postStatus("test");
    }
}
