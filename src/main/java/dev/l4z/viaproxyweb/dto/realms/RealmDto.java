package dev.l4z.viaproxyweb.dto.realms;

public record RealmDto(
        String id,
        String name,
        String owner,
        String state,
        String motd,
        String worldType,
        String version,
        boolean compatible,
        boolean expired
) {
}