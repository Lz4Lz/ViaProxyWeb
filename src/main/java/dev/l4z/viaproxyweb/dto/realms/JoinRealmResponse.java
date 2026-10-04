package dev.l4z.viaproxyweb.dto.realms;

import dev.l4z.viaproxyweb.dto.proxy.ProxyResponse;

public record JoinRealmResponse(
        String status,
        String address,
        String version,
        String networkProtocol,
        ProxyResponse proxy
) {
}