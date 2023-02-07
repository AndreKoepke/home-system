package ch.akop.homesystem.external.mastodon;

import ch.akop.homesystem.persistence.model.config.MastodonConfig;
import ch.akop.homesystem.persistence.repository.config.MastodonConfigRepository;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

@RequiredArgsConstructor
public class MastodonHeader implements ClientHeadersFactory {

    protected final MastodonConfigRepository mastodonConfigRepository;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        var result = new MultivaluedHashMap<String, String>();
        var token = mastodonConfigRepository.findFirstByOrderByModifiedDesc()
                .map(MastodonConfig::getToken)
                .orElseThrow(() -> new IllegalStateException("No API key"));
        result.add("Authorization", "Bearer " + token);
        return result;
    }
}
