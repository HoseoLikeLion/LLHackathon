package com.hackathon.skinroutine.common;

/** 에러 응답 계약: {"error":{"code":"...","message":"..."}} — docs/05 공통 규약 */
public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }
}
