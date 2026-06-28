package com.ceos.voteservice.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    AUTH_001(HttpStatus.UNAUTHORIZED, "아이디 혹은 비밀번호가 올바르지 않습니다."),
    AUTH_002(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
    AUTH_003(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    MEMBER_001(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    MEMBER_002(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    MEMBER_003(HttpStatus.BAD_REQUEST, "존재하지 않는 유저입니다."),
    VOTE_001(HttpStatus.CONFLICT, "이미 투표를 완료한 사용자입니다."),
    VOTE_002(HttpStatus.BAD_REQUEST, "본인 파트의 후보자에게만 투표할 수 있습니다."),
    VOTE_003(HttpStatus.BAD_REQUEST, "본인 팀에는 투표할 수 없습니다."),
    CANDIDATE_001(HttpStatus.BAD_REQUEST, "존재하지 않는 후보자입니다."),
    COMMON_001(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    COMMON_999(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
