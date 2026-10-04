package dev.l4z.viaproxyweb.dto.realms;

import java.util.List;

public record RealmsListResponse(
        boolean compatible,
        String type,
        String version,
        String account,
        List<RealmDto> realms
) {
}