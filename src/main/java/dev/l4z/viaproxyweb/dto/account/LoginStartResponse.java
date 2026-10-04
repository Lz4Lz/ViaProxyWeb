package dev.l4z.viaproxyweb.dto.account;

public record LoginStartResponse(
        String loginId,
        String userCode,
        String verificationUri,
        String directVerificationUri
) {
}