package dev.l4z.viaproxyweb.dto.account;

public record LoginStatusResponse(
        Status status,
        String accountName,
        String error
) {

    public enum Status {
        UNKNOWN,
        PENDING,
        SUCCESS,
        ERROR
    }

    public static LoginStatusResponse unknown() {
        return new LoginStatusResponse(
                Status.UNKNOWN,
                null,
                null
        );
    }


    public static LoginStatusResponse pending(
            String userCode,
            String verificationUri,
            String directVerificationUri
    ) {
        return new LoginStatusResponse(
                Status.PENDING,
                null,
                null
        );
    }


    public static LoginStatusResponse success(String accountName) {
        return new LoginStatusResponse(
                Status.SUCCESS,
                accountName != null ? accountName : "",
                null
        );
    }

    public static LoginStatusResponse error(String error) {
        return new LoginStatusResponse(
                Status.ERROR,
                null,
                error != null ? error : "Unknown error"
        );
    }
}