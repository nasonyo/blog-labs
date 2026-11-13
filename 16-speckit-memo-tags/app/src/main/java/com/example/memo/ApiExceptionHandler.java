package com.example.memo;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 400 응답 본문. 기존 기본 오류 본문의 필드(timestamp, status, error, path)는 그대로 두고
 * 문제가 된 입력을 알려주는 errors 를 추가한다 (Constitution II, IV).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    public record FieldError(String field, String message) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return badRequest(request, errors);
    }

    @ExceptionHandler(InvalidTagException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTag(
            InvalidTagException e, HttpServletRequest request) {
        return badRequest(request, List.of(new FieldError(e.getField(), e.getMessage())));
    }

    /** JSON 이 깨졌거나 필드의 타입이 맞지 않을 때. 타입이 틀린 필드를 알 수 있으면 그 이름을, 아니면 body 를 알려준다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        if (e.getCause() instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            StringBuilder field = new StringBuilder();
            for (JsonMappingException.Reference ref : jme.getPath()) {
                if (ref.getFieldName() != null) {
                    if (field.length() > 0) field.append('.');
                    field.append(ref.getFieldName());
                } else {
                    field.append('[').append(ref.getIndex()).append(']');
                }
            }
            return badRequest(request, List.of(new FieldError(field.toString(), "값의 형식이 올바르지 않습니다")));
        }
        return badRequest(request, List.of(new FieldError("body", "요청 본문을 읽을 수 없습니다 (JSON 형식을 확인하세요)")));
    }

    private ResponseEntity<Map<String, Object>> badRequest(HttpServletRequest request, List<FieldError> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("path", request.getRequestURI());
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }
}
