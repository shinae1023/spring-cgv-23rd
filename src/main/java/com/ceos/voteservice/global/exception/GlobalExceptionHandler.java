package com.ceos.voteservice.global.exception;

import com.ceos.voteservice.global.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException exception) {
        return toResponse(ErrorCode.AUTH_001);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.COMMON_001.getMessage());

        return toResponse(ErrorCode.COMMON_001, message);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException exception) {
        ErrorCode errorCode = exception.getMessage().contains("아이디")
                ? ErrorCode.MEMBER_001
                : ErrorCode.MEMBER_002;

        return toResponse(errorCode, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return toResponse(ErrorCode.VOTE_001);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        ErrorCode errorCode = resolveIllegalArgumentErrorCode(exception.getMessage());
        return toResponse(errorCode, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        return toResponse(ErrorCode.COMMON_999);
    }

    private ErrorCode resolveIllegalArgumentErrorCode(String message) {
        if (message.contains("파트장 투표는 1회") || message.contains("팀 투표는 1회")) {
            return ErrorCode.VOTE_001;
        }
        if (message.contains("본인 파트")) {
            return ErrorCode.VOTE_002;
        }
        if (message.contains("본인 팀")) {
            return ErrorCode.VOTE_003;
        }
        if (message.contains("후보자")) {
            return ErrorCode.CANDIDATE_001;
        }
        if (message.contains("유저")) {
            return ErrorCode.MEMBER_003;
        }
        if (message.contains("인증")) {
            return ErrorCode.AUTH_002;
        }

        return ErrorCode.COMMON_001;
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return toResponse(errorCode, errorCode.getMessage());
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.name(), message));
    }
}
