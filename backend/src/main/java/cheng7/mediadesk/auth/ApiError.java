package cheng7.mediadesk.auth;

public record ApiError(
    String code,
    String message,
    String requestId
) {
}
