package org.ipredencao.lambda.model;

public record LambdaResponse(
    boolean success,
    ResponseData data,
    String error
) {
    public record ResponseData(
        String uid,
        String email,
        String name,
        String displayName,
        Boolean emailVerified,
        String token
    ) {}
    
    public static LambdaResponse success(ResponseData data) {
        return new LambdaResponse(true, data, null);
    }
    
    public static LambdaResponse error(String msg) {
        return new LambdaResponse(false, null, msg);
    }
}
