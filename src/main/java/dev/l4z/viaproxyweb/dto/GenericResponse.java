package dev.l4z.viaproxyweb.dto;

public record GenericResponse(
        String status, // error, success ...
        String message
) {
}
