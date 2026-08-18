package com.hackathon.skinroutine.common;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 계약 형식 {"error":{"code","message"}} 으로 변환한다.
 * 인증 실패(X-User-Id 없음/무효)는 401 — docs/05 공통 규약.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    /** X-User-Id 헤더 자체가 없음 → 401 */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
        if ("X-User-Id".equalsIgnoreCase(e.getHeaderName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(
                    "MISSING_USER_ID", "X-User-Id 헤더가 필요해요. POST /api/users 로 먼저 ID를 발급받아 주세요."));
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("MISSING_HEADER", e.getHeaderName() + " 헤더가 필요해요."));
    }

    /** 값 형식 오류 — UUID 파라미터는 X-User-Id 뿐이라 UUID 실패는 401로 처리 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (UUID.class.equals(e.getRequiredType())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(
                    "INVALID_USER_ID", "X-User-Id 형식이 잘못됐어요. 서버가 발급한 ID를 그대로 보내주세요."));
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_PARAMETER", "요청 값 형식이 잘못됐어요: " + e.getName()));
    }

    /** 컨트롤러 파라미터 검증 실패 (@Min/@Max 등 — Spring 6.1 내장 메서드 검증) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().isEmpty()
                ? "요청 값이 올바르지 않아요." : e.getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", message));
    }

    /** @RequestBody @Valid 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "요청 값이 올바르지 않아요." : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("MISSING_PARAMETER", e.getParameterName() + " 값이 필요해요."));
    }

    /** multipart에서 photo 파트 누락 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("MISSING_PART", e.getRequestPartName() + " 파일이 필요해요."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of("FILE_TOO_LARGE", "사진이 너무 커요. 10MB 이하로 올려주세요."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_BODY", "요청 본문(JSON)을 읽을 수 없어요."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", "Content-Type을 확인해 주세요. (#3은 multipart/form-data)"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("METHOD_NOT_ALLOWED", "허용되지 않은 HTTP 메서드예요: " + e.getMethod()));
    }

    /** 존재하지 않는 경로 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "없는 경로예요: /" + e.getResourcePath()));
    }

    /** UNIQUE 제약 등 DB 충돌 — 서비스에서 못 잡은 동시성 경쟁의 최종 방어선 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("DB 제약 충돌: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DATA_CONFLICT", "이미 존재하는 데이터예요."));
    }

    /** 그 외 전부 — 원인은 서버 로그에 남기고, 클라이언트에는 내부 정보를 노출하지 않는다 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 서버 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했어요. 잠시 후 다시 시도해 주세요."));
    }
}
