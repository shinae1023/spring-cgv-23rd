package cgv_23rd.ceos.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode {

    // 인증 에러
    DUPLICATE_LOGINID(HttpStatus.BAD_REQUEST,"AUTH_4001","중복되는 아이디가 존재합니다."),
    MISSING_AUTH_INFO(HttpStatus.UNAUTHORIZED, "AUTH_4011", "인증 정보가 누락되었습니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "AUTH_4012", "올바르지 않은 아이디, 혹은 비밀번호입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_4012", "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_4031", "접근 권한이 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_4191", "토큰이 만료되었습니다."),

    // 서버 내부 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_5001", "서버 내부 오류입니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVER_5031", "서버가 일시적으로 불안정합니다."),
    EXTERNAL_SERVICE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "SERVER_5041", "외부 서비스 응답 지연"),

    //요청 파라미터 에러
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "REQ_4001", "필수 파라미터가 누락되었습니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "REQ_4002", "파라미터 형식이 잘못되었습니다."),
    UNSUPPORTED_CONTENT_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "REQ_4151", "지원하지 않는 Content-Type입니다."),

    //유저 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"USER_4041","유저를 찾을 수 없습니다."),

    //영화 에러
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND,"MOVIE_4041","영화를 찾을 수 없습니다."),
    INVALID_MOVIE_DATE(HttpStatus.BAD_REQUEST, "MOVIE_4001", "종영일은 개봉일 이후여야 합니다."),

    //영화관 에러
    THEATER_NOT_FOUND(HttpStatus.NOT_FOUND,"THEATER_4041", "극장을 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND,"SEAT_4041","좌석을 찾을 수 없습니다."),
    SEAT_SCREEN_INVALID(HttpStatus.BAD_REQUEST, "SEAT_4001","해당 상영관의 좌석이 아닙니다."),
    MOVIESCREEN_NOT_FOUND(HttpStatus.NOT_FOUND,"MOVIESCREEN_4041","상영 회차를 찾을 수 없습니다."),
    SCREEN_NOT_FOUND(HttpStatus.NOT_FOUND,"SCREEN_4041","상영관을 찾을 수 없습니다."),

    // 상영 시간표 에러
    INVALID_SCHEDULE_TIME(HttpStatus.BAD_REQUEST, "SCHEDULE_4001", "종료 시간이 시작 시간보다 빠를 수 없습니다."),
    SCHEDULE_OVERLAPPED(HttpStatus.BAD_REQUEST, "SCHEDULE_4002", "해당 상영관에 시간이 겹치는 상영 일정이 존재합니다."),
    SCREEN_THEATER_MISMATCH(HttpStatus.BAD_REQUEST,"SCHEDULE_4003", "해당 극장에 속한 상영관이 아닙니다."),

    //예매 에러
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_4041", "예매를 찾을 수 없습니다."),
    RESERVATION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST,"RESERVATION_4001","이미 취소된 예매입니다."),
    RESERVATION_SEAT_EMPTY(HttpStatus.BAD_REQUEST,"RESERVATION_4002", "예매할 좌석을 선택해주세요."),
    RESERVATION_SEAT_DUPLICATION(HttpStatus.BAD_REQUEST,"RESERVATION_4002", "이미 예약된 좌석입니다."),
    MOVIE_ALREADY_STARTED(HttpStatus.BAD_REQUEST,"RESERVATION_4003","이미 상영이 시작된 회차는 예매/취소할 수 없습니다."),

    // 리뷰 에러
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REVIEW_4001", "이미 해당 영화에 대한 리뷰를 작성했습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_4041", "리뷰를 찾을 수 없습니다."),

    // 매점/음식 에러
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "FOOD_4041", "해당 음식을 찾을 수 없습니다."),
    THEATER_FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "FOOD_4042", "해당 극장에서 판매하지 않는 음식입니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "FOOD_4001", "음식 재고가 부족합니다."),
    FOOD_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOOD_ORDER_4041", "주문 내역을 찾을 수 없습니다."),
    FOOD_ORDER_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "FOOD_ORDER_4001", "이미 취소된 주문입니다."),
    FOOD_ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "FOOD_ORDER_4002", "완료된 주문은 취소할 수 없습니다."),
    FOOD_ORDER_INVALID_STATE(HttpStatus.BAD_REQUEST, "FOOD_ORDER_4003", "주문 상태가 유효하지 않습니다."),

    // 결제 에러
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_4001", "결제에 실패했습니다."),
    PAYMENT_NOT_READY(HttpStatus.BAD_REQUEST, "PAYMENT_4002", "결제를 진행할 수 없는 상태입니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "PAYMENT_4091", "이미 결제 처리된 예매입니다."),
    PAYMENT_NOT_CANCELLABLE(HttpStatus.CONFLICT, "PAYMENT_4092", "취소할 수 없는 결제 상태입니다."),
    PAYMENT_SERVER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_5001", "결제 처리 중 서버 오류가 발생했습니다."),

    //락 에러
    RESERVATION_ALREADY_LOCKED(HttpStatus.BAD_REQUEST,"RESERVATION_4004","이미 다른 사용자가 예매를 진행 중인 좌석입니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "LOCK_5001", "락 획득에 실패했습니다."),
    LOCK_ACQUISITION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"LOCK_5002","락 획득 중 오류가 발생했습니다."),


    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
