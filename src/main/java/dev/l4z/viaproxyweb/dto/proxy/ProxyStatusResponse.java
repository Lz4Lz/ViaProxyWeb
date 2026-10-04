package dev.l4z.viaproxyweb.dto.proxy;

public record ProxyStatusResponse(
        boolean running,
        int clients,
        String account,
        int accountIndex,
        String target
) {
}