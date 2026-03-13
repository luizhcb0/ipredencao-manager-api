package org.ipredencao.lambda.model;

public record LambdaRequest(
    String operation,
    RequestData data
) {
    public record RequestData(
        String token,
        String email,
        String password,
        String displayName,
        String uid
    ) {}
}
