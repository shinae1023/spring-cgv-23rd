## 트랜잭션 전파 속성
### REQUIRED 
: 스프링 트랜잭션의 기본 설정 이미 진행 중인 트랜잭션이 있으면 기존 트랜잭션에 합류하고, 없으면 새로운 물리 트랜잭션을 생성함 여러 논리 트랜잭션이 하나의 물리 트랜잭션을 공유하게 됨

```
@Service
@RequiredArgsConstructor 
public class OrderService {

    private PaymentService paymentService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void processOrder(Order order) {
        orderRepository.save(order);   
        // 기존 트랜잭션에 합류
        paymentService.processPayment(order); 
    }
}

@Service
public class PaymentService {
    @Transactional(propagation = Propagation.REQUIRED)
    public void processPayment(Order order) {
        paymentRepository.save(new Payment(order.getId(), order.getAmount()));
    }
}
```

```
processOrder 시작 (신규 물리 트랜잭션 1 시작, rollbackOnly = false)
    ├── orderRepository.save()    
    │
    ├── processPayment() 호출 (물리 트랜잭션 1에 합류)
    │       ├── paymentRepository.save() 
    │       ├──  예외 발생
    │       └── 트랜잭션 매니저가 rollbackOnly = true 로 마킹
    │
    ├── 예외가 processOrder로 전파됨
    │
    └── processOrder 종료 시점
            ↓
        rollbackOnly = true 확인
            ↓
        물리 트랜잭션 1 전체 ROLLBACK 
            ↓
        주문, 결제 모두 롤백됨
```

여러 서비스 로직을 완벽하게 하나의 원자적 작업(All or Nothing)으로 묶어서, 하나라도 실패하면 전체를 취소해야 할 때 사용

### REQUIRES_NEW
: 현재 트랜잭션의 존재 여부와 상관없이 항상 새로운 물리 트랜잭션을 시작 기존 트랜잭션이 있으면 이를 일시 정지시키고, 새로운 커넥션을 맺어 독립적으로 실행

```
@Service
@RequiredArgsConstructor 
public class OrderService {

    private LogService logService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void processOrder(Order order) {
        orderRepository.save(order);
        
        try {
            // 새 물리 트랜잭션 시작
            logService.writeLog("주문 시도: " + order.getId());
        } catch (RuntimeException e) {
            // 예외를 잡아서 부모 트랜잭션으로 전파되는 것을 막음
            log.error("로그 저장 실패");
        }
    }
}

@Service
public class LogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeLog(String message) {
        logRepository.save(new Log(message));
    }
}

```

```
processOrder 시작 (신규 물리 트랜잭션 1 시작)
    ├── orderRepository.save()     
    │
    ├── writeLog() 시작
    │       ├── 물리 트랜잭션 1 일시 정지
    │       ├── 신규 물리 트랜잭션 2 시작 (새 DB Connection)
    │       ├── 예외 발생
    │       └── 물리 트랜잭션 2 ROLLBACK (로그만 롤백)
    │
    ├── 예외가 processOrder로 던져짐 -> catch문으로 잡음 
    │       ↓
    │   물리 트랜잭션 1 재개
    │
    └── processOrder 종료 시점
            ↓
        rollbackOnly = false 유지됨 (예외를 잡았으므로)
            ↓
        물리 트랜잭션 1 COMMIT 
            ↓
        로그는 롤백되었지만 주문은 정상 커밋됨

```

메인 비즈니스 로직의 성공/실패 여부와 무관하게 반드시 커밋되어야 하는 로직(예: 시스템 로그, 통계 데이터 적재)을 분리할 때 사용 단, DB 커넥션을 2개 이상 동시에 사용하므로 트래픽이 많을 때 커넥션 풀(Connection Pool)이 빠르게 고갈될 수 있어 주의가 필요

### MANDATORY
  : 기존 트랜잭션이 반드시 있어야 함. 이미 진행 중인 트랜잭션이 있으면 합류하고, 없으면 예외(IllegalTransactionStateException)를 발생시킴

```
@Service
@RequiredArgsConstructor 
public class OrderService {

    private LogService logService;

    // 트랜잭션 없음
    public void processOrder(Order order) {
        // ① 로그 저장 (오류 발생)
        logService.writeLog("주문 시도: " + order.getId()); 
    }
}

@Service
public class LogService {
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeLog(String message) {
        logRepository.save(new Log(message));
    }
}
```

```
트랜잭션 흐름 (부모 트랜잭션이 없을 경우)

Plaintext
processOrder 시작 (트랜잭션 X)
    ├── writeLog() 호출
    │       ↓
    │   트랜잭션 매니저 확인: "진행 중인 트랜잭션이 없네?"
    │       ↓
    │   IllegalTransactionStateException 예외 발생 
    │       ↓
    └── processOrder 비정상 종료 (로그 저장 실패)
= 강제적으로 부모 트랜잭션 내에서만 실행되도록 보장해야 할 때 사용
```

### SUPPORTS
: 기존 트랜잭션이 있으면 합류하고, 없으면 트랜잭션 없이(Non-Transactional) 단순하게 실행

```
@Service
@RequiredArgsConstructor 
public class OrderService {

    private SelectService selectService;

    // 트랜잭션 없음
    public void getOrderInfo() {
        selectService.readData();
    }
}

@Service
public class SelectService {
    @Transactional(propagation = Propagation.SUPPORTS)
    public void readData() {
        // 데이터 조회 로직
    }
}
```

```
트랜잭션 흐름 (부모 트랜잭션이 없을 경우)

Plaintext
getOrderInfo 시작 (트랜잭션 X)
    ├── readData() 호출
    │       ↓
    │   트랜잭션 매니저 확인: "진행 중인 트랜잭션 없음"
    │       ↓
    │   신규 트랜잭션 생성 안 함 (단순 쿼리 실행) 
    │       ↓
    └── getOrderInfo 종료
```

읽기 전용 메서드처럼 굳이 트랜잭션 오버헤드가 필요 없지만, 부모 트랜잭션이 있다면 같은 영속성 컨텍스트를 공유하고 싶을 때 사용

### NOT_SUPPORTED
: 트랜잭션을 지원하지 않음. 기존 진행 중인 트랜잭션이 있으면 이를 일시 정지하고, 트랜잭션 없이(Non-Transactional) 실행

```
@Service
@RequiredArgsConstructor 
public class OrderService {

    private ExternalApiService externalApiService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void processOrder(Order order) {
        orderRepository.save(order);
        // 트랜잭션 일시 정지 후 외부 API 호출
        externalApiService.sendSms("주문 완료");
    }
}

@Service
public class ExternalApiService {
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendSms(String message) {
        // 외부 SMS 발송 로직 (DB 트랜잭션 무관)
    }
}
```

```
processOrder 시작 (신규 트랜잭션 1 시작)
    ├── orderRepository.save()      영속성 컨텍스트에 적재
    │
    ├── sendSms() 시작
    │       ├── 물리 트랜잭션 1 일시 정지
    │       ├── 트랜잭션 없이 외부 API 호출 실행 
    │       └── 완료 후 물리 트랜잭션 1 재개
    │
    └── processOrder 종료 → 트랜잭션 1 COMMIT 
```
DB Connection을 오래 쥐고 있으면 안 되는 외부 API 호출이나, 긴 작업 시간이 소요되는 트랜잭션 무관 로직을 처리할 때 사용

### NEVER
: 트랜잭션 환경을 절대 허용하지 않고 기존 진행 중인 트랜잭션이 있으면 예외(IllegalTransactionStateException)를 발생시킴 없으면 트랜잭션 없이 실행

```
@Service
@RequiredArgsConstructor 
public class OrderService {

    private VerifyService verifyService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void processOrder(Order order) {
        // 검증 로직 호출 (오류 발생)
        verifyService.checkSomething();
    }
}

@Service
public class VerifyService {
    @Transactional(propagation = Propagation.NEVER)
    public void checkSomething() {
        // 무거운 비즈니스 로직
    }
}
```

```
processOrder 시작 (신규 트랜잭션 시작)
    ├── checkSomething() 호출
    │       ↓
    │   트랜잭션 매니저 확인: "트랜잭션이 진행 중이네?"
    │       ↓
    │   IllegalTransactionStateException 예외 발생 
    │       ↓
    └── processOrder 비정상 종료 (전체 롤백)
```

트랜잭션이 적용되면 치명적인 데드락이 발생하거나 성능 저하가 우려되는 핵심 로직을 보호하기 위해 강제 차단할 때 사용

### NESTED
: 기존 트랜잭션이 있으면 중첩 트랜잭션을 시작함. (기존 트랜잭션이 없으면 REQUIRED와 동일하게 작동)
물리 트랜잭션은 부모와 1개를 공유하지만 중첩 지점에 DB Savepoint를 생성하여 자식 트랜잭션 부분만 별도로 롤백 가능

```
@Service
@RequiredArgsConstructor 
public class OrderService {

    private LogService logService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void processOrder(Order order) {
        orderRepository.save(order);
        
        try {
            // 자식 트랜잭션 호출
            logService.writeLog("주문 완료");
        } catch (RuntimeException e) {
            // 예외를 잡아서 부모 트랜잭션으로 전파되지 않게 막음
            log.error("로그 저장 실패, 하지만 주문은 진행함");
        }
    }
}

@Service
public class LogService {
    @Transactional(propagation = Propagation.NESTED)
    public void writeLog(String message) {
        logRepository.save(new Log(message));
        throw new RuntimeException("로그 저장 중 오류 발생");
    }
}
```

```
processOrder 시작 (신규 물리 트랜잭션 1 시작)
    ├── orderRepository.save()      
    │
    ├── writeLog() 시작
    │       ├── 물리 트랜잭션 1 내부에 Savepoint 생성
    │       ├── logRepository.save() 
    │       ├──  예외 발생
    │       └── Savepoint 지점까지만 ROLLBACK (부모에게 영향 없음)
    │
    ├── 예외가 processOrder로 던져짐 -> catch문으로 잡음 
    │
    └── processOrder 종료
            ↓
        rollbackOnly = false 유지됨
            ↓
        물리 트랜잭션 1 COMMIT 
            ↓
        주문은 커밋, 로그는 롤백됨

```

REQUIRES_NEW보다 DB Connection 낭비를 줄이면서, 부분 롤백(특정 작업 실패 시 메인 작업은 유지)이 필요할 때 사용 (단, JPA에서는 지원하지 않으며 JDBC Savepoint 기능에 의존)

## 트랜잭션 개선 사항

이번 리팩토링에서는 결제/취소/만료 스케줄러 흐름을 중심으로 트랜잭션 경계를 다시 정리함
목표는 다음 3가지 :

- 외부 API 호출과 DB 트랜잭션을 명확히 분리
- 클래스 레벨 `@Transactional` 남용을 줄이고 메서드 단위로 경계를 드러냄
- 상태 전이 규칙을 강화해 잘못된 결제 상태 변경을 막음

---

### 1. 예매 취소 플로우에서 외부 결제 취소를 트랜잭션 밖으로 분리

기존 `ReservationService.cancelPaidReservation()`은 클래스 레벨 `@Transactional` 안에서 바로 외부 결제 취소 API를 호출하고 있었음

이 구조는 다음 문제가 있었음

- 외부 네트워크 대기 시간 동안 트랜잭션 경계가 길어짐
- 결제 성공 플로우와 취소 플로우의 설계 원칙이 일관되지 않음
- 서비스 계층이 외부 API 호출과 로컬 상태 변경을 동시에 책임짐

이를 개선하기 위해 역할을 다음처럼 분리함

- `ReservationPaymentFacade`
  외부 PG 취소 API 호출 담당
- `ReservationService`
  외부 취소가 성공한 뒤 로컬 DB 상태 변경만 담당

즉, 흐름은 아래처럼 바뀌었다.

1. `ReservationPaymentFacade`에서 완료 예매 여부 확인
2. `paymentService.cancelPayment(...)` 호출
3. 취소 성공 응답 확인
4. 그 후에만 `reservationService.cancelPaidReservationAfterPaymentCancellation(...)` 호출
5. 짧은 로컬 트랜잭션에서 `paymentStatus=CANCELLED`, `status=취소` 반영

이로써 외부 API 호출과 DB 상태 변경의 책임이 분리되었고, 긴 트랜잭션을 줄일 수 있게 됨

### 2. 클래스 레벨 `@Transactional` 제거 및 메서드 단위 경계 명시

기존에는 일부 서비스에 클래스 레벨 `@Transactional`이 걸려 있었음

- `ReservationService`
- `FoodOrderService`
- `ReviewService`

이 방식은 빠르게 적용하기는 쉽지만, 다음 문제가 있는데

- 새 메서드가 추가될 때 의도치 않게 모두 쓰기 트랜잭션이 됨
- 내부 호출(self-invocation) 때문에 메서드별 어노테이션 의도가 흐려질 수 있음
- 읽기/쓰기 메서드의 구분이 코드에서 잘 드러나지 않음

개선 후에는 다음 원칙으로 바꿈

- 쓰기 메서드에만 `@Transactional`
- 조회 메서드는 `@Transactional(readOnly = true)`
- 클래스 레벨 선언은 제거

예를 들어 다음 메서드들만 명시적으로 트랜잭션을 갖도록 정리함

- 예매 생성, 결제 준비, 결제 확정, 취소
- 매점 주문 생성, 결제 준비, 재고 차감, 취소
- 리뷰 생성

이렇게 하면 트랜잭션 경계가 코드에서 더 분명하게 드러나고, 추후 유지보수 시 실수 가능성이 감소

### 3. 만료 스케줄러의 내부 호출 트랜잭션 문제 분리

기존 `PendingOrderExpirationService`는 다음 구조였음

- `expirePendingReservationsAndFoodOrders()`
  내부에서
  - `expirePendingReservations()`
  - `expirePendingFoodOrders()`
    호출

하지만 같은 클래스 내부 호출은 스프링 프록시를 타지 않기 때문에, 안쪽 메서드의 `@Transactional`이 독립적으로 적용되지 않음

즉, 선언상으로는 분리된 것처럼 보여도 실제로는 하나의 큰 트랜잭션처럼 동작할 수 있음

이를 해결하기 위해 만료 책임을 분리하는 두 개의 서비스로 나눔

- `PendingReservationExpirationService`
- `PendingFoodOrderExpirationService`

그리고 `PendingOrderExpirationService`는 두 서비스를 호출하는 오케스트레이션 역할만 담당하게 함

이 구조의 장점 : 

- 예약 만료와 매점 주문 만료의 트랜잭션 경계가 실제로 분리
- 한쪽 실패가 다른 쪽 처리에 불필요하게 영향을 주는 구조를 줄일 수 있음
- 코드상 의도와 실제 런타임 동작이 일치

### 4. 보상 트랜잭션의 락 정책 통일

보상 처리용 `PaymentCompensationService`는 `REQUIRES_NEW`를 사용해 독립 트랜잭션으로 보상 로직을 수행함

기존에는 다음 차이가 있었는데

- 예매 보상 취소는 `findByIdWithLock(...)`
- 매점 주문 보상 취소는 일반 `findById(...)`

이 차이는 동시성 상황에서 음식 주문 보상 처리 쪽이 더 약한 구조이므로
개선 후에는 매점 주문 보상 취소도 `findByIdWithLock(...)`로 맞춤

이렇게 맞춘 이유 : 

- 보상 로직도 결국 상태 변경이므로 락 정책이 일관되어야 함
- 사용자 취소, 스케줄러 만료, 보상 처리 등이 겹칠 때 상태 경합을 줄일 수 있음
- 실패 복구 경로는 특히 더 보수적으로 설계하는 편이 안전함

### 5. 주문/예매의 결제 상태 전이 규칙 강화

기존에도 `assignPaymentId`, `confirm` 등 일부 검증은 있었지만, `markPaymentPaid`, `markPaymentFailed`, `markPaymentUnknown`, `markPaymentCancelled`는 상대적으로 전이 규칙이 느슨했음

이 상태에서는 아래와 같은 문제가 생길 수 있음

- 이미 완료된 주문/예매에 잘못된 결제 상태를 덮어쓸 수 있음
- `PROCESSING`이 아닌 상태에서 `FAILED`, `UNKNOWN`으로 바뀌는 실수를 막기 어려움
- 보상/예외 처리 중 중복 호출이 일어날 때 의도치 않은 상태 변경이 생길 수 있음

이를 개선하기 위해 `Reservation`, `FoodOrder` 엔티티에 상태 전이 검증을 추가힘

- `PAID` 전이
  `PROCESSING -> PAID`만 허용
- `FAILED` 전이
  `PROCESSING -> FAILED`만 허용
- `UNKNOWN` 전이
  `PROCESSING -> UNKNOWN`만 허용
- `CANCELLED` 전이
  `PAID` 또는 `UNKNOWN` 상태에서만 허용
- 잘못된 전이는 `PAYMENT_NOT_READY` 예외로 차단
- 이미 같은 상태인 경우 일부 메서드는 idempotent 하게 무시

이로써 서비스 계층에서 한 번 더 방어하고, 최종적으로는 도메인 엔티티가 자기 상태를 스스로 지킬 수 있게 함

### 최종 효과

- 외부 결제 호출과 DB 트랜잭션 경계가 더 명확해짐
- 취소 플로우가 결제 플로우와 동일한 설계 원칙
- 클래스 레벨 트랜잭션으로 인한 과도한 경계 확장을 감소
- 스케줄러 만료 처리의 실제 트랜잭션 동작이 선언과 일치
- 보상 처리의 동시성 안정성이 높아짐
- 결제 상태 전이 규칙이 강화되어 잘못된 상태 변경 가능성이 줄어듦

---
## 쿼리 성능 최적화

이번 과제에서는 실제 서비스에서 자주 사용되거나 성능 병목이 발생할 수 있는 쿼리를 선정한 뒤,  
`EXPLAIN ANALYZE`를 통해 실행 계획을 직접 확인하고 인덱스를 개선하여 성능을 최적화했다.

최적화 대상은 다음 3가지 쿼리이다.

1. 예약 만료 대상 조회 쿼리
2. 사용자별 예매 목록 조회 쿼리
3. 상영 시간 겹침 검사 쿼리

### 1. 예약 만료 대상 조회 쿼리 최적화

#### 대상 쿼리

```sql
SELECT id
FROM reservation
WHERE status = '대기'
  AND payment_status IN ('READY', 'FAILED')
  AND created_at < NOW() + INTERVAL 1 DAY;
```

#### 기존 문제점

기존에는 `reservation(status, created_at)` 인덱스를 사용하고 있었기 때문에  
`status`, `created_at` 조건은 인덱스로 처리할 수 있었지만,  
`payment_status` 조건은 별도의 `Filter` 단계에서 후처리되고 있었다.

기존 실행 계획 일부:

```text
Filter: (reservation.payment_status in ('READY','FAILED'))
Index range scan on reservation using idx_reservation_status_created_at
```

즉 쿼리 조건은 `status`, `payment_status`, `created_at`를 모두 사용하지만,  
기존 인덱스는 `payment_status`를 반영하지 못해 조건과 인덱스 구성이 완전히 일치하지 않았다.

#### 적용한 인덱스

```sql
CREATE INDEX idx_reservation_status_payment_created_id
ON reservation (status, payment_status, created_at, id);
```

#### 개선 결과

복합 인덱스 적용 후 실행 계획:

```text
Covering index range scan on reservation using idx_reservation_status_payment_created_id
```

즉 `status`, `payment_status`, `created_at`를 모두 인덱스에서 처리할 수 있게 되었고,  
조회 컬럼인 `id`까지 인덱스에 포함되어 테이블 본문 접근 없이 결과를 반환할 수 있게 되었다.

#### 정리

- 기존: `payment_status` 후처리 필터 발생
- 개선 후: `Covering index range scan`
- 의미: 조건과 인덱스 구조의 정합성이 높아짐

---

### 2. 사용자별 예매 목록 조회 쿼리 최적화

#### 대상 쿼리

```sql
SELECT id
FROM reservation
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 20;
```

#### 기존 문제점

기존에는 `user_id`에 대한 외래키 인덱스는 존재했지만,  
정렬 기준인 `created_at DESC`가 인덱스에 포함되어 있지 않아 추가 정렬이 발생했다.

기존 실행 계획:

```text
Index lookup on reservation using FK... (user_id=1)
Sort: reservation.created_at DESC
```

즉 사용자별 예매는 빠르게 찾을 수 있었지만,  
최신순으로 정렬하기 위해 별도의 `Sort` 비용이 들었다.

#### 적용한 인덱스

```sql
CREATE INDEX idx_reservation_user_created_id
ON reservation (user_id, created_at, id);
```

#### 개선 결과

개선 후 실행 계획:

```text
Covering index lookup on reservation using idx_reservation_user_created_id (user_id=1) (reverse)
```

실행 시간 비교:

- 최적화 전: `2.57 ms`
- 최적화 후: `0.665 ms`

#### 정리

- 기존: `user_id` 조건 탐색 후 별도 정렬 수행
- 개선 후: 복합 인덱스를 통해 최신순 조회를 인덱스 수준에서 처리
- 의미: `WHERE + ORDER BY + LIMIT` 패턴 최적화 성공

---

### 3. 상영 시간 겹침 검사 쿼리 최적화

#### 대상 쿼리

```sql
SELECT COUNT(*) > 0
FROM movie_screen ms
WHERE ms.screen_id = 1
  AND ms.start_at < '2026-12-01 20:00:00'
  AND ms.end_at > '2026-12-01 19:00:00';
```

이 쿼리는 상영 일정 등록 시,  
해당 상영관에 시간 겹침이 존재하는지 검사하는 데 사용된다.

#### 기존 문제점

기존에는 `screen_id` 외래키 인덱스를 통해 상영관별 후보를 찾은 뒤,  
`start_at`, `end_at` 조건은 후처리 필터로 검사하고 있었다.

기존 실행 계획:

```text
Index lookup on ms using FK... (screen_id=1)
Filter: (ms.start_at < ... and ms.end_at > ...)
```

즉 상영관 단위로는 빠르게 찾지만,  
시간 조건은 인덱스 차원에서 충분히 활용하지 못하고 있었다.

#### 적용한 인덱스

```sql
CREATE INDEX idx_movie_screen_screen_start_end
ON movie_screen (screen_id, start_at, end_at);
```

#### 개선 결과

개선 후 실행 계획:

```text
Covering index range scan on ms using idx_movie_screen_screen_start_end
```

실행 시간 비교:

- 최적화 전: `0.124 ms`
- 최적화 후: `0.052 ms`

#### 정리

- 기존: `screen_id`만 인덱스로 찾고 시간 조건은 필터 처리
- 개선 후: `screen_id + start_at + end_at` 복합 인덱스를 활용한 범위 탐색
- 의미: 일정 충돌 검사 비용 감소

---

## 실행 계획 및 성능 비교 요약

| 쿼리 | 기존 방식 | 개선 방식 | 최적화 전 | 최적화 후 |
|---|---|---|---:|---:|
| 예약 만료 대상 조회 | `status`, `created_at` 인덱스 + `payment_status` 후처리 | `status`, `payment_status`, `created_at`, `id` 복합 인덱스 | 실행시간 차이 미미 | `Covering index range scan` 확인 |
| 사용자별 예매 목록 조회 | `user_id` 인덱스 탐색 후 정렬 | `user_id`, `created_at`, `id` 복합 인덱스 | `2.57 ms` | `0.665 ms` |
| 상영 시간 겹침 검사 | `screen_id` 인덱스 탐색 후 시간 필터 | `screen_id`, `start_at`, `end_at` 복합 인덱스 | `0.124 ms` | `0.052 ms` |

---

## 결론

이번 최적화에서는 단순히 인덱스를 추가하는 데 그치지 않고,  
실제 쿼리의 조건 순서와 접근 패턴에 맞게 복합 인덱스를 설계했다.

특히 다음과 같은 점을 확인할 수 있었다.

- 조건 컬럼이 인덱스에 충분히 반영되지 않으면 후처리 `Filter`가 남는다.
- `WHERE + ORDER BY + LIMIT` 패턴은 정렬 컬럼까지 포함한 복합 인덱스가 매우 중요하다.
- `Covering index scan`이 가능해지면 테이블 본문 접근 없이 더 효율적인 조회가 가능하다.



## 운영 로깅 개선

### 이번 리팩토링에서 적용한 내용

- 요청 단위 추적을 위해 `RequestContextLoggingFilter`를 추가하고 모든 요청에 `requestId`, `method`, `uri`, `clientIp`를 MDC에 저장
- JWT 인증 성공 시 `userId`, `userEmail`을 MDC에 추가해서 이후 서비스/예외 로그에서 같은 요청 흐름을 함께 추적 가능하게 구성
- `logback-spring.xml`을 JSON 구조화 로그 기반으로 변경하고 운영 로그 `application.log`, 감사 로그 `logs/audit/audit.log`를 분리
- 결제/예매/매점 결제 흐름 로그를 `event` 중심으로 통일해서 Loki/Grafana에서 `event`, `errorCode`, `requestId` 기준 검색이 가능하도록 정리
- `ExceptionAdvice`, 인증 실패, 인가 실패 로그에도 `event`, `uri`, `method`, `errorCode`를 담아 장애 분석 시 HTTP 요청 문맥이 남도록 개선
- `alloy-config.alloy`를 JSON 로그 파싱 방식으로 변경해서 `level`, `event`, `errorCode`, `logType`를 Loki label로 활용할 수 있게 구성

### 주요 로그 이벤트

- 운영 로그: `request_completed`, `jwt_authenticated`, `business_exception`, `validation_failed`, `payment_instant_requested`, `payment_instant_completed`, `food_payment_failed`, `reservation_payment_completed`
- 감사 로그: `food_payment_completed`, `food_payment_cancelled`, `reservation_payment_completed`, `reservation_payment_cancelled`

### 기대 효과

- 장애 발생 시 `requestId` 하나로 인증 → 서비스 → 예외 흐름을 연결해서 추적 가능
- 결제 실패를 `errorCode`, `paymentId`, `orderId`, `reservationId` 기준으로 바로 필터링 가능
- 일반 운영 로그와 돈이 관련된 감사 로그를 분리해서 보관 정책과 접근 제어를 다르게 가져갈 수 있음
<img width="2848" height="4464" alt="image" src="https://github.com/user-attachments/assets/e7ce532a-89f0-430d-a9b2-c272dd86fc14" />

## 캐싱과 로깅

### Redis Cache 적용 내역

- `MovieQueryService#getMovieList()`: 현재 상영 중 영화 목록은 메인 화면에서 반복 조회될 가능성이 높아서 `movieList` 캐시 적용
- `MovieQueryService#getMovieDetail()`: 영화 상세 정보는 이미지와 통계 정보를 함께 조회하므로 `movieDetail` 캐시 적용
- `MovieQueryService#getMovieActors()`: 출연진 정보는 자주 바뀌지 않는 조회 데이터라서 `movieActors` 캐시 적용
- `TheaterQueryService#getTheatersByRegion()`: 지역별 극장 목록은 같은 조건으로 재조회될 가능성이 높아 `theatersByRegion` 캐시 적용
- `TheaterQueryService#getTheaterDetail()`: 극장 상세는 상대적으로 변경이 적은 조회라 `theaterDetail` 캐시 적용
- `ScheduleQueryService#getSchedules()`: 극장과 날짜 기준 시간표 조회는 호출 빈도가 높고 조건이 명확해서 `schedules` 캐시 적용
- `ReviewQueryService#getMovieReviews()`: 리뷰 목록은 반복 조회가 많아 `movieReviews` 캐시를 적용했고, 최신성을 위해 TTL을 짧게 적용

### Redis Cache 무효화 내역

- `AdminMovieService#createMovie()`: 영화 생성 후 현재 상영작 목록이 바뀔 수 있어서 `movieList` 캐시 무효화
- `AdminMovieService#createTheater()`: 극장 생성 후 해당 지역 목록이 달라지므로 `theatersByRegion` 캐시 무효화
- `AdminMovieService#createSchedule()`: 시간표 등록 후 같은 극장/날짜 조회 결과가 달라지므로 해당 `schedules` 캐시 무효화
- `ReviewService#createReview()`: 리뷰 작성 후 리뷰 목록과 영화 평점 정보가 달라질 수 있어서 `movieReviews`, `movieDetail` 캐시 무효화

<details>
<summary><h1>❓</h1></summary>
  
## 1. EntityManager는 누가 생성하고, DB와의 연결은 어떻게 이루어질까요?

### 1) EntityManager의 생성 주체

`EntityManager`는 직접 생성x → JPA의 공장 역할을 하는 `EntityManagerFactory`에 의해 생성

- **EntityManagerFactory :** 애플리케이션 로딩 시점에 DB당 하나만 생성됨. 설정 정보(persistence.xml 등)를 바탕으로 DB 연결에 필요한 무거운 객체를 구성함.
- **EntityManager :** 고객의 요청(트랜잭션 단위)이 올 때마다 `EntityManagerFactory`에서 생성함.
- **생성 방식 :**
    - **J2SE(일반 자바) 환경 :** `factory.createEntityManager()`를 호출하여 직접 생성함.
    - **Spring/EE 환경 :** `@PersistenceContext` 어노테이션을 사용하면 컨테이너(Spring)가 생성과 주입을 자동으로 관리함.

### 2) DB와의 연결 방식

`EntityManager`는 생성되자마자 DB 연결을 맺는 것이 아님. 효율적인 자원 관리를 위해 필요한 시점(Lazy)에 연결을 수행함.

- **연결 시점 :** 보통 트랜잭션이 시작되거나, 실제 DB 조회가 필요한 시점에 커넥션 풀에서 커넥션을 획득
- **커넥션 풀 활용 :** `EntityManagerFactory`가 생성될 때 미리 설정된 커넥션 풀(HikariCP 등)을 확보해 둠 `EntityManager`는 이 풀에서 커넥션을 빌려 쓰고, 작업이 끝나면 다시 반납
- **영속성 컨텍스트 :** `EntityManager` 내부에 존재하며, 엔티티를 영구 저장하는 환경으로 DB 연결 전 단계에서 1차 캐시 역할을 수행함.

### 3-1) Spring Data JPA에서의 사용 : EntityManager 프록시 객체 주입

- **프록시 활용 :** `@PersistenceContext`나 생성자 주입으로 받는 `EntityManager`는 실제 객체가 아닌 스프링이 생성한 **프록시 객체**임
- **동적 바인딩 :** 메서드 호출 시점에 **트랜잭션 동기화 매니저**를 조회하여 현재 트랜잭션에 할당된 실제 `EntityManager`에 작업을 위임함
- **스레드 안전성 :** 프록시를 통해 각 요청(스레드)마다 별도의 실제 `EntityManager`를 연결하므로 멀티스레드 환경에서도 안전하게 사용 가능

### 3-2) 트랜잭션 및 영속성 컨텍스트 관리

- **트랜잭션 시작 :** `@Transactional` 어노테이션이 붙은 메서드 실행 시 `TransactionInterceptor`가 트랜잭션을 개시
- **EntityManager 생성 :** 트랜잭션 시작 시점에 `EntityManagerFactory`로부터 `EntityManager`를 생성하고 영속성 컨텍스트를 구축
- **커넥션 획득 :** 실제 DB 조작이 필요한 시점(Lazy)에 커넥션 풀에서 DB 커넥션을 획득하여 사용
- **자원 반납 :** 트랜잭션 종료(커밋/롤백) 시 `EntityManager`를 닫고 커넥션을 풀에 반납

---

### 3-3) JpaRepository와 EntityManager의 관계

- **구현체 내부 동작 :** `JpaRepository`의 기본 구현체인 `SimpleJpaRepository` 내부에 `EntityManager`가 필드로 포함
- **기능 위임 :** `save()`, `find()`, `delete()` 등의 메서드 호출 시 주입된 프록시 `EntityManager`의 표준 JPA 메서드를 호출하여 처리

## 2. flush의 발생하는 시점은 언제일까요?

### 1) 직접 호출 (`em.flush()`)

- **강제 반영 :** 개발자가 코드상에서 명시적으로 `flush()`를 호출하여 영속성 컨텍스트의 변경 내용을 즉시 DB에 동기화
- **사용 사례 :** 트랜잭션이 끝나기 전, 중간에 SQL이 실행되는 것을 확인하고 싶거나 특정 로직 직전에 DB 반영이 필요할 때 사용

### 2) 트랜잭션 커밋 시 자동 호출

- **동기화 보장 :** JPA는 트랜잭션을 커밋하기 직전에 자동으로 `flush`를 호출
- **이유 :** 영속성 컨텍스트의 변경 내용(Insert, Update, Delete)을 DB에 보내지 않고 커밋하면, DB에는 아무런 변화가 일어나지 않기 때문에 반드시 커밋 전에 SQL을 전달해야 함

### 3) JPQL 쿼리 실행 직전 자동 호출

- **데이터 일관성 유지 :** JPQL은 영속성 컨텍스트를 거치지 않고 DB에 직접 SQL을 던짐
- **문제 방지 :** 만약 영속성 컨텍스트에서 엔티티를 수정했는데 `flush` 되지 않은 상태에서 JPQL로 해당 데이터를 조회하면, 수정 전의 옛날 데이터가 조회되는 문제가 발생
- **해결 방식 :** 이런 데이터 불일치를 막기 위해 JPA는 JPQL 실행 전 무조건 `flush`를 수행하여 최신 상태를 DB에 반영

### 4) 주의사항

- **flush ≠ commit :** `flush`는 영속성 컨텍스트의 변경 내용을 DB에 전달(SQL 실행)하는 과정일 뿐, 트랜잭션을 완전히 종료하는 `commit`과는 다름.
- **롤백 가능 :** `flush`가 호출되어 SQL이 DB에 전달되었더라도, 트랜잭션이 커밋되기 전이라면 언제든 롤백이 가능

## 3.  **JOIN을 사용할 때 SQL과 JPQL이 어떤 기준으로 조인을 수행하는지** 비교해보면 차이를 더 쉽게 이해할 수 있어요

### 1) SQL의 조인 기준 (테이블 중심)

**SQL은 데이터베이스의 테이블과 외래 키(FK)를 기준으로 조인을 수행**

- **조인 대상 :** 물리적인 테이블 간의 결합
- **기준 컬럼 :** 외래 키와 기본 키의 값이 일치하는지를 명시적으로 확인해야 함
- **명시적 조건 :** ON 절을 통해 어떤 컬럼끼리 매칭할지 개발자가 직접 작성해야 함

```sql
SELECT M.*, T.*
FROM MEMBER M
JOIN TEAM T ON M.TEAM_ID = T.ID  -- 외래 키 컬럼 기준
```

### 2) JPQL의 조인 기준 (엔티티 객체 중심)

**JPQL은 데이터베이스 테이블이 아닌 엔티티 객체와 그들 간의 연관관계 필드를 기준으로 조인을 수행**

- **조인 대상 :** 객체 그래프를 탐색하는 과정
- **기준 필드 :** 엔티티 클래스 내에 선언된 `@ManyToOne` 등의 **연관관계 필드**
- **암묵적 매핑 :** 이미 엔티티 설정에서 외래 키 매핑이 완료되었으므로, JPQL에서는 `m.team`처럼 객체 참조를 통해 조인 대상을 지정

```sql
SELECT m FROM Member m 
JOIN m.team t  -- Member 엔티티 내부의 team 필드(연관관계) 기준
```

### 3) Fetch Type과의 연결점

- **즉시 로딩 (Eager) :** JPQL로 엔티티를 조회할 때, JPA가 SQL을 생성하면서 연관된 테이블까지 미리 `JOIN` 쿼리에 포함시켜 한꺼번에 가져옴
- **지연 로딩 (Lazy) :** JPQL 실행 시점에는 해당 엔티티만 조회하고, 연관된 객체는 실제 사용되는 시점에 별도의 SQL을 실행하여 가져옴 (프록시 객체 활용)

이처럼 JPQL은 객체 지향적인 관점에서 쿼리를 작성하면, JPA가 이를 해석하여 데이터베이스 관점의 SQL 조인문으로 변환해주는 역할을 수행

## 4. fetch join을 사용하면서 페이징을 적용할 때 발생하는 문제에 대해 알아보아요!

### 1) 데이터 뻥튀기(Cartesian Product) 문제

- **중복 발생 :** DB 입장에서 '일(1)' 측 엔티티와 '다(N)' 측 엔티티를 조인하면, 결과 Row 수는 '다' 측의 개수에 맞춰서 늘어남
- **기준 상실 :** 예를 들어 팀 1개에 멤버가 3명 있다면, 조인 결과는 3개의 로우가 나옴. 이때 JPA는 "팀 1개를 가져와서 1페이지에 보여줘"라는 요청을 받았을 때, 3개의 로우 중 어디까지가 팀 1개인지 DB 레벨에서 계산하기 어려움

### 2) 인메모리 페이징 (HHH000104 경고)

- **하이버네이트의 처리 :** 하이버네이트는 DB 단에서 페이징 쿼리(`LIMIT`, `OFFSET`)를 날리는 것이 불가능하다고 판단하면, **모든 데이터를 메모리로 읽어온 뒤 애플리케이션 메모리에서 페이징을 처리**
- **위험성 :** 데이터가 수만 건 이상일 경우 `OutOfMemoryError`가 발생하여 서버가 다운될 수 있는 매우 위험한 상황
- **로그 확인 :** 실행 시 `firstResult/maxResults specified with collection fetch; applying in memory!`라는 경고 로그가 출력됨.

### 3) 해결 방법

- Batch Size 설정 (가장 권장됨)
    - **원리 :** 컬렉션 페치 조인을 포기하는 대신, 지연 로딩을 유지하면서 `IN` 절을 통해 설정한 개수만큼 한꺼번에 조회
    - **설정 방법 :** * 글로벌 설정: `application.yml`에 `hibernate.default_batch_fetch_size: 100` 추가
        
        개별 설정: 연관관계 필드 위에 `@BatchSize(size = 100)` 
        
    - **장점 :** N+1 문제를 해결하면서 페이징 쿼리도 정상적으로 DB에서 실행
- ToOne 관계만 페치 조인
    - **구분 :** `@ManyToOne`, `@OneToOne`은 데이터 뻥튀기가 발생하지 않으므로 페이징과 페치 조인을 함께 써도 무방
    - **전략 :** 'ToOne' 관계는 페치 조인으로 한 번에 가져오고, 컬렉션('ToMany')은 위에서 언급한 `Batch Size`로 처리
- DTO 직접 조회
    - **방식 :** 엔티티를 조회하지 않고 필요한 필드만 뽑아서 DTO로 변환하여 조회
    - **특징 :** 복잡한 통계성 쿼리나 페이징이 복잡할 때 성능 최적화에 유리

## 5. SimpleJpaRepository의 EntityManager 주입

싱글톤 객체가 상태를 가지면 스레드 세이프(Thread-safe)하지 않다? → 하지만 스프링은 **프록시 패턴**을 통해 이를 해결

- **프록시 객체 주입 :** `SimpleJpaRepository` 생성자에서 주입받는 `EntityManager`는 실제 DB에 연결된 객체가 아니라, 스프링이 만든 **공유 프록시(Shared Entity Manager)** 객체
- **동작 원리 :** 사용자가 리포지토리 메서드를 호출하면, 프록시 객체가 현재 스레드에 할당된 트랜잭션 동기화 매니저에서 해당 트랜잭션 전용 `EntityManager`를 찾아서 실제 작업을 위임
- **결론 :** 싱글톤 리포지토리는 하나의 가짜 프록시를 들고 있고, 실제 호출 시점에만 진짜 `EntityManager`를 연결하므로 동시성 문제가 발생하지 않음.

## 6. Fetch Join 시 `distinct`를 사용하지 않을 때의 문제

일대다(@OneToMany) 관계에서 페치 조인을 수행하면 SQL 결과에서 데이터 중복(Cartesian Product)이 발생함.

- **객체 중복 :** DB에서 팀(1)과 멤버(3)를 조인하면 로우가 3개 조회됨. JPA는 이를 그대로 읽어와서 결과 리스트에 동일한 팀 엔티티 객체를 3개 담아 반환
- **해결 방법 :** `select distinct t from Team t join fetch t.members`와 같이 `distinct`를 추가함.
    - **SQL distinct :** SQL 레벨에서 중복 제거를 시도함 (하지만 모든 컬럼 값이 같아야 하므로 효과가 미비할 수 있음)
    - **JPA distinct :** 하이버네이트가 애플리케이션 레벨에서 동일한 식별자(ID)를 가진 엔티티의 중복을 제거
    - *참고: Hibernate 6 버전부터는 `distinct`를 명시하지 않아도 엔티티 조회 시* 자동으로 중복을 제거

## 7. Fetch Join 관련 3대 에러 원인 및 해결 방안

### 1) HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!

- **원인 :** 일대다 컬렉션 페치 조인과 페이징(`Pageable`)을 동시에 사용했기 때문임. DB 레벨에서 조인으로 인해 로우 수가 늘어나 정확한 페이지 계산이 불가능하므로, 하이버네이트가 데이터를 전부 메모리로 퍼올려 페이징을 시도
- **해결 :** 페치 조인을 제거하고 **`hibernate.default_batch_fetch_size`** 설정을 통해 지연 로딩을 최적화

### 2) query specified join fetching, but the owner of the fetched association was not present in the select list

- **원인 :** 페치 조인을 사용하면서 `SELECT` 절에 조인의 기준이 되는 엔티티를 포함하지 않았을 때 발생함.
    - 예: `select m.name from Member m join fetch m.team` (Member 엔티티 자체가 아닌 필드만 조회하면서 페치 조인을 시도함)
- **해결 :** 페치 조인은 연관된 엔티티를 영속성 컨텍스트에 한꺼번에 올리기 위한 용도이므로, **반드시 기준 엔티티 자체를 조회**해야 함 (`select m from Member m ...`)

### 3) org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags

- **원인 :** 한 번의 쿼리에서 2개 이상의 일대다 컬렉션을 동시에 페치 조인하려고 할 때 발생함. 데이터가 기하급수적으로 늘어나는 카테시안 곱의 위험 때문에 하이버네이트가 이를 차단
- **해결 :** 1. 컬렉션 타입을 `List` 대신 `Set`으로 변경함 (단, 데이터 순서가 보장되지 않고 여전히 카테시안 곱 문제는 존재)
    
    2. 가장 권장되는 방법은 하나만 페치 조인하고 나머지는 **`Batch Size`** 설정을 통해 여러 번의 쿼리로 나누어 가져오는 것
</details>



<details>
<summary><h1>CGV 클론코딩 ERD 연관관계 및 제약조건 정리</h1></summary>
  
## 🎬 CGV 클론코딩 
### 📌 핵심 기능
- 영화관 조회 및 찜
- 영화 조회 (상세 정보, 리뷰, 통계 포함)
- 영화 예매 및 취소
- 좌석 선택 (상영관 기준)
- 영화 찜 기능
- 매점 주문 (극장별 재고 관리)

### ERD
https://www.erdcloud.com/d/br5ZwPJDjX9WvPYXH
<img width="1044" height="451" alt="image" src="https://github.com/user-attachments/assets/d1361fe0-e23c-4bdb-9f1b-1823a2ef6360" />

## 1. 전체 엔티티 연관관계 정리

## 1-1. 영화관 / 상영관 / 좌석 관련

### Theater(극장) - Screen(상영관)

* **관계**: `1 : N`
* **설명**: 하나의 극장은 여러 개의 상영관을 가진다.
* **FK**: `Screen.theaterId`

### ScreenType(상영관 타입) - Screen(상영관)

* **관계**: `1 : N`
* **설명**: 하나의 상영관 타입(일반관, IMAX 등)은 여러 상영관에 적용될 수 있다.
* **FK**: `Screen.screenTypeId`

### ScreenType(상영관 타입) - SeatTemplate(좌석 템플릿)

* **관계**: `1 : N`
* **설명**: 하나의 상영관 타입은 여러 좌석 템플릿 정보를 가진다.
* **FK**: `SeatTemplate.screenTypeId`

### Screen(상영관) - Seat(좌석)

* **관계**: `1 : N`
* **설명**: 하나의 상영관은 여러 좌석을 가진다.
* **FK**: `Seat.screenId`

### Screen(상영관) - MovieScreen(영화상영)

* **관계**: `1 : N`
* **설명**: 하나의 상영관에서는 여러 상영 회차가 존재할 수 있다.
* **FK**: `MovieScreen.screenId`

---

## 1-2. 영화 관련

### Movie(영화) - MovieImage(영화사진)

* **관계**: `1 : N`
* **설명**: 하나의 영화는 여러 장의 이미지를 가질 수 있다.
* **FK**: `MovieImage.movieId`

### Movie(영화) - MovieStatistics(영화통계)

* **관계**: `1 : 1`
* **설명**: 하나의 영화는 하나의 통계 정보를 가진다.
* **FK**: `MovieStatistics.movieId`

### Movie(영화) - MovieScreen(영화상영)

* **관계**: `1 : N`
* **설명**: 하나의 영화는 여러 상영 회차를 가질 수 있다.
* **FK**: `MovieScreen.movieId`

### Movie(영화) - Review(리뷰)

* **관계**: `1 : N`
* **설명**: 하나의 영화에는 여러 리뷰가 작성될 수 있다.
* **FK**: `Review.movieId`

### Movie(영화) - MovieLike(영화찜)

* **관계**: `1 : N`
* **설명**: 하나의 영화는 여러 사용자에게 찜될 수 있다.
* **FK**: `MovieLike.movieId`

### Movie(영화) - MovieActor(영화출연자)

* **관계**: `1 : N`
* **설명**: 하나의 영화에는 여러 출연자/감독 정보가 연결될 수 있다.
* **FK**: `MovieActor.movieId`

### Actor(출연자) - MovieActor(영화출연자)

* **관계**: `1 : N`
* **설명**: 하나의 출연자/감독은 여러 영화에 연결될 수 있다.
* **FK**: `MovieActor.actorId`

> 따라서 `Movie` 와 `Actor` 는 `MovieActor` 를 통한 **N : M 관계**이다.

---

## 1-3. 예매 관련

### User(유저) - Reservation(예매)

* **관계**: `1 : N`
* **설명**: 하나의 유저는 여러 예매를 할 수 있다.
* **FK**: `Reservation.userId`

### MovieScreen(영화상영) - Reservation(예매)

* **관계**: `1 : N`
* **설명**: 하나의 상영 회차에는 여러 예매가 발생할 수 있다.
* **FK**: `Reservation.movieScreenId`

### Reservation(예매) - ReservationSeat(예매좌석)

* **관계**: `1 : N`
* **설명**: 하나의 예매는 여러 좌석을 포함할 수 있다.
* **FK**: `ReservationSeat.reservationId`

### Seat(좌석) - ReservationSeat(예매좌석)

* **관계**: `1 : N`
* **설명**: 하나의 좌석은 여러 회차에서 반복적으로 예매될 수 있다.
* **FK**: `ReservationSeat.seatId`

### MovieScreen(영화상영) - ReservationSeat(예매좌석)

* **관계**: `1 : N`
* **설명**: 하나의 상영 회차에는 여러 예약 좌석 정보가 존재할 수 있다.
* **FK**: `ReservationSeat.movieScreenId`

---

## 1-4. 영화관 찜 / 매점 관련

### User(유저) - TheaterLike(극장찜)

* **관계**: `1 : N`
* **설명**: 하나의 유저는 여러 극장을 찜할 수 있다.
* **FK**: `TheaterLike.userId`

### Theater(극장) - TheaterLike(극장찜)

* **관계**: `1 : N`
* **설명**: 하나의 극장은 여러 사용자에게 찜될 수 있다.
* **FK**: `TheaterLike.theaterId`

> 따라서 `User` 와 `Theater` 는 `TheaterLike` 를 통한 **N : M 관계**이다.

---

## 1-5. 음식 / 주문 관련

### Food(음식) - TheaterFood(극장음식)

* **관계**: `1 : N`
* **설명**: 하나의 음식은 여러 극장에서 판매될 수 있다.
* **FK**: `TheaterFood.foodId`

### Theater(극장) - TheaterFood(극장음식)

* **관계**: `1 : N`
* **설명**: 하나의 극장은 여러 음식 재고를 가진다.
* **FK**: `TheaterFood.theaterId`

> 따라서 `Food` 와 `Theater` 는 `TheaterFood` 를 통한 **N : M 관계**이며,
> `TheaterFood.amount` 로 극장별 재고를 관리한다.

### User(유저) - FoodOrder(음식구매)

* **관계**: `1 : N`
* **설명**: 하나의 유저는 여러 음식 주문을 할 수 있다.
* **FK**: `FoodOrder.userId`

### Theater(극장) - FoodOrder(음식구매)

* **관계**: `1 : N`
* **설명**: 하나의 극장에서는 여러 음식 주문이 발생할 수 있다.
* **FK**: `FoodOrder.theaterId`

### FoodOrder(음식구매) - FoodOrderItem(음식구매항목)

* **관계**: `1 : N`
* **설명**: 하나의 주문은 여러 음식 항목을 포함할 수 있다.
* **FK**: `FoodOrderItem.foodOrderId`

### Food(음식) - FoodOrderItem(음식구매항목)

* **관계**: `1 : N`
* **설명**: 하나의 음식은 여러 주문 항목에 포함될 수 있다.
* **FK**: `FoodOrderItem.foodId`

---

## 1-6. 리뷰 관련

### User(유저) - Review(리뷰)

* **관계**: `1 : N`
* **설명**: 하나의 유저는 여러 영화에 리뷰를 작성할 수 있다.
* **FK**: `Review.userId`

### Movie(영화) - Review(리뷰)

* **관계**: `1 : N`
* **설명**: 하나의 영화는 여러 리뷰를 가질 수 있다.
* **FK**: `Review.movieId`

---

## 2. ReservationSeat에 movieScreenId를 중복 저장한 이유

`ReservationSeat`에는 이미 `reservationId`가 있고,
`Reservation`에도 `movieScreenId`가 존재하므로 언뜻 보면 `ReservationSeat.movieScreenId`는 중복 데이터처럼 보임

하지만 이 중복은 **의도적인 중복**이며, 다음과 같은 이유로 사용함

### 2-1. 동일 상영 회차의 동일 좌석 중복 예매 방지

예매 시스템에서 가장 중요한 무결성 중 하나는 다음이다.

* 같은 상영 회차(`movieScreenId`)에서
* 같은 좌석(`seatId`)은
* 한 번만 예약 가능해야 한다.

이를 DB 레벨에서 보장하려면 `ReservationSeat` 테이블에 아래 복합 유니크 제약을 둘 수 있어야 함

```sql
UNIQUE (movie_screen_id, seat_id)
```

하지만 `ReservationSeat`에 `movieScreenId`가 없으면 `Reservation`을 조인해야만 상영 회차를 알 수 있으므로, 단일 테이블 기준의 유니크 제약을 만들 수 없음
즉, `ReservationSeat.movieScreenId`는 **중복 예매를 DB에서 직접 차단하기 위해 필요한 중복 컬럼**

---

### 2-2. 상영 회차별 예약 좌석 조회 성능 향상

예매 화면에서는 특정 상영 회차의 예약된 좌석 목록을 자주 조회하게 됨

예시:

* 특정 회차에서 이미 예약된 좌석 조회
* 잔여 좌석 수 계산
* 좌석 선택 화면 렌더링

이때 `ReservationSeat`에 `movieScreenId`가 있으면 다음과 같이 바로 조회할 수 있음

```sql
SELECT seat_id
FROM reservation_seat
WHERE movie_screen_id = ?;
```

즉, 조인 없이 단순 조회가 가능해져 **쿼리가 단순해지고 성능상 이점**이 있다.

---

### 2-3. 정리

따라서 `ReservationSeat.movieScreenId`는 단순 중복이 아니라,

* **중복 예매 방지**
* **조회 성능 향상**
* **회차별 좌석 점유 상태를 명확하게 표현**

을 위한 **의도된 비정규화**

단, 이 구조를 사용할 경우 아래 두 가지는 서비스 로직에서 반드시 검증 필요

1. `Reservation.movieScreenId == ReservationSeat.movieScreenId`
2. `ReservationSeat.seatId`가 속한 `Seat.screenId == ReservationSeat.movieScreenId`가 속한 `MovieScreen.screenId`

즉, 예매의 상영 회차와 예매좌석의 상영 회차가 같아야 하고 선택한 좌석은 해당 상영 회차의 상영관 좌석이어야 함.

---

## 3. 유니크 제약 조건 정리

아래 유니크 제약은 데이터 무결성 보장을 위해 필요

---

### 3-1. ReservationSeat

동일 회차에서 동일 좌석 중복 예매 방지

```sql
UNIQUE (movie_screen_id, seat_id)
```

---

### 3-2. Seat

하나의 상영관 안에서 같은 좌석 위치 중복 생성 방지

```sql
UNIQUE (screen_id, row_name, col_num)
```

예시:

* 같은 상영관에 `A열 1번` 좌석이 두 번 생성되면 안 됨

---

### 3-3. MovieLike

한 사용자가 같은 영화를 여러 번 찜하는 것 방지

```sql
UNIQUE (user_id, movie_id)
```

---

### 3-4. TheaterLike

한 사용자가 같은 극장을 여러 번 찜하는 것 방지

```sql
UNIQUE (user_id, theater_id)
```

---

### 3-5. Review

한 사용자가 같은 영화에 리뷰를 여러 번 작성하지 못하도록 제한 (정책이 "영화당 리뷰 1개"인 경우)

```sql
UNIQUE (user_id, movie_id)
```

---

### 3-6. MovieStatistics

하나의 영화에 하나의 통계만 존재하도록 보장

```sql
UNIQUE (movie_id)
```

---

### 3-7. MovieScreen

동일 상영관에서 같은 시작 시간의 중복 상영 방지
(필요 시)

```sql
UNIQUE (screen_id, start_at)
```

> 다만 상영 시간 겹침 자체는 단순 유니크만으로 완벽히 막을 수 없으므로,
> 실제로는 서비스 로직에서 `start_at ~ end_at` 겹침 검증이 추가로 필요하다.

---

### 3-8. MovieImage

대표 이미지 정렬이나 중복 관리가 필요하다면 다음과 같은 제약 고려 가능

예:

```sql
UNIQUE (movie_id, movie_image_url)
```

또는

```sql
UNIQUE (movie_id, sort_order)
```

---

## 4. 서비스 로직에서 추가로 검증해야 하는 부분

일부 제약은 DB만으로 완벽히 표현하기 어렵기 때문에 서비스 계층에서 추가 검증 필요

### 4-1. 예매 좌석 무결성 검증

* `Reservation.movieScreenId == ReservationSeat.movieScreenId`
* `Seat.screenId == MovieScreen.screenId`

즉, 예매와 예매좌석의 회차가 일치해야 하며 좌석은 해당 상영관의 좌석이어야 함

---

### 4-2. 상영 시간 겹침 검증

같은 상영관에서 상영 시간이 겹치는 회차는 등록되면 안 됨

예:

* 10:00 ~ 12:00 상영이 있는데
* 11:30 ~ 13:30 상영을 추가하면 안 됨

이는 단순 유니크로는 막기 어렵고 서비스 로직에서 시간 범위 겹침 검사 필요

---

### 4-3. 음식 재고 검증

`TheaterFood.amount`는 극장별 음식 재고를 의미하므로

* 주문 시 재고 이상 구매 불가
* 구매 완료 시 재고 차감
* 환불 불가 정책이면 주문 취소에 따른 재고 복구 없음

이 정책을 서비스 로직에서 관리해야 함.

## 5. @MapsId 사용 이유 (MovieStatistics 엔티티 적용)

- **식별 관계(Identifying Relationship) 구현**: 부모 엔티티(`Movie`)의 기본 키를 자식 엔티티(`MovieStatistics`)가 자신의 기본 키이자 외래 키로 공유함.
- **DB 저장 공간 최적화**: 별도의 PK 컬럼을 생성하지 않고 부모의 PK를 그대로 사용하므로 저장 공간을 절약하고 인덱스 구조를 단순화함.
- **조회 성능 및 편의성**: `Movie`의 ID만 알면 `MovieStatistics`를 조인 없이 또는 아주 단순한 조건으로 바로 식별할 수 있음.

### 5-1. 구현 방식

- **코드 적용**: `MovieStatistics` 엔티티에서 `Movie` 필드에 `@OneToOne`과 `@MapsId`를 함께 선언함.
- **동작 원리**: JPA는 `MovieStatistics`를 저장할 때 연결된 `Movie` 객체의 ID 값을 추출하여 `MovieStatistics`의 PK(id 필드) 값으로 설정함.

### 5-2. 다른 연결 방식 (대안)

- **일반적인 @OneToOne (비식별 관계)**:
    - **방식**: `@MapsId` 없이 `@JoinColumn(name = "movie_id")`만 사용함.
    - **특징**: `MovieStatistics`가 별도의 고유한 PK(예: `Long id`)를 가지며, `movie_id`는 단순 외래 키 컬럼으로 존재함.
    - **단점**: 1:1 관계임에도 불구하고 두 엔티티의 ID가 달라 관리가 복잡해질 수 있으며, 외래 키에 대한 유니크 제약 조건을 별도로 설정해야 함.

</details>

<details>
  <summary><h1>영화 예매 기능</h1></summary>
  
  ## 1. SeatTemplate를 사용한 이유: 좌석의 규격화 및 자동화

`SeatTemplate`은 상영관의 물리적인 좌석을 생성하기 위한 **설계도** 역할을 수행

- **상영관 타입별 배치 관리**: IMAX, 일반관 등 상영관 타입(`ScreenType`)에 따라 좌석 배치(행, 열)가 다르므로, 이를 템플릿화하여 관리
- **좌석 생성 자동화**: 새로운 상영관을 등록할 때 일일이 좌석을 수동으로 입력하지 않고, 연결된 `ScreenType`의 `SeatTemplate` 정보를 참조하여 실제 `Seat` 데이터를 일괄 생성할 수 있는 기반이 됨.
- **유지보수 효율성**: 상영관 타입의 표준 좌석 배치가 변경될 경우 템플릿만 수정하면 되므로 관리가 용이

### 1-1. Seat과 ReservationSeat을 구분하여 사용한 이유

`Seat`은 마스터 데이터(정적 정보)이고, `ReservationSeat`은 트랜잭션 데이터(동적 정보)로 성격이 완전히 다르기 때문에 분리하여 설계함

- **마스터 데이터와 이력 데이터의 분리**:
    - **Seat**: 상영관에 고정된 물리적 좌석 그 자체 (예: A열 1번).
    - **ReservationSeat**: "누가, 어떤 영화 회차에, 얼마를 내고 해당 좌석을 점유했는가"에 대한 예매 이력
- **상태 독립성 보장**: 특정 회차의 예매가 취소되거나 완료되어도 물리적인 좌석(`Seat`) 정보는 변하지 않음. 오직 해당 회차의 점유 기록인 `ReservationSeat`만 생성 또는 삭제(취소 시 clear)됨.
- **가격 정보의 스냅샷 저장**: 영화 가격은 조조, 주말, 상영관 타입에 따라 변동될 수 있음. `ReservationSeat`에 예매 당시의 가격(`price`)을 직접 저장함으로써, 나중에 영화 가격 정책이 바뀌어도 과거 예매 총액을 정확히 보존함.

## 2. 예매 서비스 관련 핵심 설계 사항

### 2-1. 동시성 및 무결성 제약 조건

- **복합 유니크 제약**: `Seat` 테이블에서 동일 상영관 내 중복 위치 방지(`uk_screen_row_col`), `Review` 테이블에서 1인 1리뷰 제한(`uk_user_movie_review`) 등을 통해 데이터 무결성을 유지
- **서비스 로직 검증**: 예매 시점에 영화 상영이 이미 시작되었는지 확인(`isBefore(LocalDateTime.now())`)하고, 선택한 좌석이 해당 상영관의 좌석이 맞는지(`seat.getScreen().getId().equals(...)`) 검증

### 2-2. 예매 및 취소 로직의 특징

- **객체 그래프 관리**: 예매 취소 시 `Reservation`의 상태를 '취소'로 바꾸고 연관된 `ReservationSeat` 리스트를 비우는(`clear`) 방식을 사용하여, 영속성 컨텍스트의 `orphanRemoval` 기능을 통해 관련 데이터를 정리
- **총액 업데이트**: 예매 시 좌석별 가격을 합산하여 `Reservation` 엔티티의 `totalPrice`를 실시간으로 업데이트하고 반영

### 2-3. 예매 취소에서 DELETE 대신 POST 매핑을 사용한 이유

- **리소스의 상태 변경** : `DELETE`는 해당 리소스를 데이터베이스에서 완전히 삭제하여 더 이상 존재하지 않게 할 때 사용. 반면, 예매 취소는 '예매'라는 리소스를 삭제하는 것이 아니라, 상태를 '완료'에서 '취소'로 변경하는 행위
- **데이터 보존** : 실무에서는 환불 처리, 통계 분석, 고객 상담 등을 위해 취소된 예매 내역도 데이터베이스에 남겨두어야 함. `DELETE`를 써서 행을 지워버리면 이러한 이력 관리가 불가능
- **멱등성 및 의미론적 차이**: `POST`는 서버의 상태를 변경하는 일반적인 액션을 수행할 때 적합함. 예매 취소는 상태 값 업데이트와 더불어 좌석 점유 해제 등 여러 로직이 복합적으로 일어나는 '처리'의 개념이 강하므로 `POST`를 선택

### 2-4. delete (Repository)와 clear() (Collection)의 차이

- **delete (JpaRepository.delete)** :
    - **동작**: 데이터베이스에서 해당 엔티티에 해당하는 행을 즉시 삭제함.
    - **결과**: `Reservation` 객체 자체가 사라지므로 예매 내역 조회 시 해당 데이터를 찾을 수 없게 됨.
- **clear() (List.clear)** :
    - **동작** : 부모 엔티티(`Reservation`)와 연관된 자식 엔티티(`ReservationSeat`)들 사이의 연결 관계를 끊는 것
    - **결과** : 부모 객체인 `Reservation`은 그대로 유지되지만, 그 안의 리스트인 `reservationSeats`가 비워짐.

### 2-5. orphanRemoval = true의 역할과 활용

- **설정 상태**: `Reservation` 엔티티의 `reservationSeats` 필드에 `orphanRemoval = true`가 설정되어 있음
- **고아 객체 제거**: `list.clear()`를 호출하여 부모와의 연결이 끊어진 자식 객체(`ReservationSeat`)를 고아 객체로 간주하고, JPA가 DB에서 해당 자식 데이터들만 자동으로 `DELETE` 쿼리를 날려 삭제함.
- **코드 내 활용 방식**:
    1. 예매 내역 자체(`Reservation`)는 상태만 '취소'로 바꿔서 DB에 남겨둠.
    2. `reservationSeats.clear()`를 통해 점유하고 있던 좌석 정보만 DB에서 삭제함.
    3. 이를 통해 예매 이력은 보존하면서도, 해당 좌석은 다른 사람이 다시 예매할 수 있도록 비워주는(해제) 효과를 얻음.
       
### 2-6. 주요 테스트 시나리오

- **단위 테스트 수행**: `@ExtendWith(MockitoExtension.class)`를 사용하여 데이터베이스 연결 없이 서비스 계층의 로직만 분리하여 테스트
- **의존성 모킹(Mocking)**: `UserRepository`, `ReservationRepository`, `ReservationSeatRepository`, `MovieScreenRepository`, `SeatRepository` 등 연관된 모든 리포지토리를 가짜 객체(Mock)로 만들어 주입

테스트 코드는 영화 예매 생성(`createReservation`) 기능에 대해 두 가지 케이스 검증 : 

### ① 영화 예매 성공 테스트 (`createReservation_Success`)

- **정상 프로세스 검증**: 유효한 유저 ID, 상영 회차 ID, 좌석 ID 리스트가 주어졌을 때 예매가 정상적으로 완료되는지 확인
- **객체 확인**: 서비스 로직 내부에서 유저 및 상영 회차 정보를 조회하고, 예약된 좌석이 없는지 확인하는 과정이 올바르게 진행되는지 점검
- **최종 결과 검증**:
    - 반환된 응답 메시지가 "예매 성공"인지 확인
    - `reservationRepository.save()`가 실제로 호출되었는지 확인하여 예매 데이터의 영속화 시도를 검증

### ② 이미 예약된 좌석일 경우 실패 테스트 (`createReservation_Fail_AlreadyReserved`)

- **중복 예매 방지 로직 검증**: 특정 상영 회차에 이미 점유된 좌석을 선택했을 때 시스템이 예외를 던지는지 확인
- **상황 시뮬레이션**: `reservationSeatRepository`의 좌석 존재 여부 확인 메서드가 `true`를 반환하도록 설정하여 이미 예약된 상황을 가정
- **예외 코드 확인**:
    - `GeneralException`이 발생하는지 확인
    - 발생한 예외의 에러 코드가 `RESERVATION_SEAT_DUPLICATION`(이미 예약된 좌석)인지 대조
    → 해당 에러코드가 발생해야 성공하는 테스트

### 테스트 코드 작성 회고 및 느낀 점

- **테스트 방식의 전환과 효율성 체감**
    - 기존 Swagger나 Postman을 활용한 테스트는 매번 애플리케이션을 실행하고 DB 환경을 설정해야 하는 번거로움이 있었음.
    - `Mockito`를 활용한 단위 테스트를 도입함으로써 별도의 설정 없이 비즈니스 로직만 고립시켜 빠르게 검증할 수 있는 환경을 구축
- **예외 처리 검증의 중요성 인지**
    - 단순히 기능이 성공하는 케이스뿐만 아니라, 경계값 확인 및 의도한 예외가 정확히 발생하는지 검증하는 과정이 필수적임을 알게 됨.
    - 실제 중복 예약 실패 테스트 도중, 기대했던 `RESERVATION_SEAT_DUPLICATION` 대신 `INVALID_PARAMETER` 에러가 발생하는 로직 오류를 발견하고 수정
- **로직 설계 및 디버깅 능력 향상**
    - 수동 테스트로는 발견하기 어려운 세밀한 에러 코드 불일치 등을 테스트 코드를 통해 잡아내며 로직의 정합성을 높임
    - 테스트 실패가 단순한 오류가 아니라, 작성한 코드의 논리적 결함을 시각화해주는 중요한 지표임을 경험함
- **향후 발전 방향**
    - 개발 시간의 상당 부분을 테스트 코드 작성에 할애하는 현업의 방식을 이해하게 되었으며, 현재의 기초적인 테스트 케이스를 넘어선 다각도의 시나리오 설계가 필요함을 알게됨
    - 다음 프로젝트에서는 해피 케이스 뿐만 아니라 더욱 촘촘한 예외 상황을 포함하여 테스트 커버리지를 넓혀나갈 계획
</details>

<details><summary><h1>비관적 락</h1></summary>
  
  ### 0. 비관적 락(Pessimistic Lock)이란

- **정의**: 데이터 충돌이 발생할 가능성이 높다고 '비관적'으로 가정하여, 데이터를 읽는 시점에 즉시 DB 수준에서 락(Lock)을 획득하는 동시성 제어 방식임.
- **동작 방식**:
    - `SELECT ... FOR UPDATE`와 같은 쿼리를 사용하여 특정 데이터 행을 점유함.
    - 조회한 트랜잭션이 완료(Commit 또는 Rollback)될 때까지 다른 트랜잭션은 해당 데이터에 대한 수정이나 읽기 시도를 차단당하고 대기함.
- **핵심 목적**: 데이터의 정합성이 무엇보다 중요한 결제나 자원 예약 시스템에서 충돌을 사전에 차단하여 무결성을 유지함.

### 1. 코드 내 비관적 락 설정 부분

다음 두 곳에서 `LockModeType.PESSIMISTIC_WRITE`를 사용하

- **ScreenRepository**: 특정 상영관을 조회할 때 `findByIdWithLock` 메서드를 통해 쓰기 락을 획득
- **TheaterFoodRepository**: 특정 극장의 음식 재고를 조회할 때 `findByTheaterAndFood` 메서드에 락을 설정

### 2. 설정 이유 (데이터 무결성 보장)

- **상영 일정 중복 등록 방지**: `ScheduleService`에서 새로운 상영 시간표를 등록할 때, 여러 요청이 동시에 같은 상영관의 시간 겹침 여부를 확인하고 등록하는 과정에서 발생할 수 있는 '레이스 컨디션'을 막기 위함임. 상영관 엔티티를 점유하여 한 번에 하나의 트랜잭션만 시간표를 수정할 수 있게 보장함.
- **음식 재고 차감의 정확성**: `FoodOrderService`에서 음식을 주문할 때, 여러 사용자가 동시에 같은 상품을 구매하려 하면 재고 수량이 부정확하게 계산되거나 음수 재고가 발생할 수 있음. 이를 방지하기 위해 해당 재고 로우를 잠금 처리하여 순차적으로 차감하도록 설계

### 3. 비관적 락의 단점

- **성능 저하**: DB 수준에서 실제 락을 걸기 때문에 다른 트랜잭션들이 해당 락이 해제될 때까지 대기해야 하므로 시스템의 전반적인 처리량이 감소
- **데드락(Deadlock) 위험**: 두 개 이상의 트랜잭션이 서로가 가진 자원의 락이 풀리기를 무한정 기다리는 교착 상태가 발생할 수 있음.
- **자원 점유 시간 증가**: 트랜잭션이 길어질수록 락을 유지하는 시간도 길어져 커넥션 풀 부족 현상 발생 가능

### 4. 대안 및 다른 방식

- **낙관적 락(Optimistic Lock)**: 데이터 충돌이 드물 것이라고 가정하는 방식임. 엔티티에 `@Version` 필드를 추가하여 수정 시점에 버전이 일치하는지 확인하며, DB 락 대신 애플리케이션 레벨에서 충돌을 감지하므로 성능상 유리
- **분산 락(Distributed Lock)**: Redis를 활용하여 DB 외부에서 락을 관리하는 방식임. DB의 부하를 줄일 수 있고, 여러 서버로 구성된 분산 환경에서 특정 자원(상영관, 재고 등)에 대한 동기화를 효율적으로 처리할 수 있음
- **원자적 쿼리(Atomic Query)**: "UPDATE ... SET amount = amount - 1"과 같이 DB 자체의 원자적 연산을 활용하여 별도의 명시적 락 없이도 재고를 안전하게 차감하는 방식을 고려

### 5. 비관적 락 사용 시 주의사항: '존재하지 않는 데이터'에 대한 잠금 한계

비관적 락(`SELECT ... FOR UPDATE`)은 데이터베이스가 락을 거는 대상과 메커니즘의 특성상 다음과 같은 주의가 필요함.

- **락의 대상은 실제 존재하는 레코드임**: 
    - **레코드 기반 잠금**: 비관적 락은 쿼리 결과로 조회된 실제 데이터 행에 물리적인 잠금을 거는 방식
    - **잠금 대상 부재**: `WHERE` 절 조건에 맞는 데이터가 테이블에 없어 아무런 행도 반환되지 않는다면, DB 엔진은 잠금을 설정할 실체를 찾지 못함
    - **결과**: 잠글 대상이 없으므로 락이 걸리지 않으며, 트랜잭션은 아무런 보호 장치 없이 다음 로직을 수행


- **작동 방식의 이해와 레이스 컨디션**: 
    - **잠금 시도 과정**: 쿼리 실행 시 행을 스캔하고, 행이 존재할 때만 배타적 잠금을 건 뒤 데이터를 반환
    - **후속 작업의 위험성**: 행이 없어 락이 안 걸린 상태라면, 다른 트랜잭션이 동시에 같은 조건으로 데이터를 `INSERT` 하더라도 이를 물리적으로 막을 수 없음

- **발생 가능한 문제점**: 
    - **동시성 제어 실패**: 특정 데이터가 없는 것을 확인하고(락이 안 걸린 상태) 새로운 데이터를 삽입(`INSERT`)하려 할 때, 그 사이 다른 트랜잭션이 먼저 데이터를 저장해버리면 **중복 데이터 오류**나 **데이터 불일치**가 발생할 수 있음.

- **해결 방안 및 보완책**: 
    - **상위 객체 잠금**: 실제 저장하려는 데이터(예: 예약 좌석)가 아직 없다면, 이미 DB에 존재하는 부모 엔티티(예: 상영관, 상품 마스터)에 비관적 락을 걸어 하위 로직 전체를 보호함.
    - **네임드 락(Named Lock)**: DB 레코드가 아닌 가상의 문자열 키를 기반으로 잠금을 획득하여 데이터 존재 여부와 상관없이 동기화를 제어함.
    - **원자적 구문 활용**: `Insert Ignore` 또는 `Upsert(ON DUPLICATE KEY UPDATE)` 구문을 사용하여 삽입 시점의 원자성을 보장함.

현재 프로젝트에서 `ScreenRepository`와 `TheaterFoodRepository`에 적용된 비관적 락은 이미 존재하는 상영관 정보나 재고 데이터를 수정할 때는 효과적이나 만약 새로운 데이터를 생성하는 로직에서 중복 여부를 체크하며 락을 걸고자 한다면, 위와 같은 메커니즘의 한계를 인지하고 부모 객체 잠금이나 분산 락 등의 대안을 병행
</details>

<details><summary><h1>JWT</h1></summary>

## 1. HTTP 상태 유지

HTTP는 각 요청이 독립적인 **Stateless** 프로토콜로 이를 해결하기 위해 초기에는 쿠키와 세션을 활용

### 1.1 쿠키 기반 메커니즘과 보안 속성

쿠키는 브라우저에 저장되는 4KB 이하의 작은 데이터 조각으로 서버가 응답 헤더(`Set-Cookie`)로 전달하면, 이후 모든 요청에 자동으로 포함

- **HttpOnly:** 자바스크립트(`document.cookie`)를 통한 접근을 차단하여 XSS 공격 시 토큰 탈취를 방지
- **Secure:** 오직 HTTPS 연결에서만 쿠키가 전송되도록 강제하여 패킷 감청을 차단
- **SameSite:** 교차 출처(Cross-site) 요청 시 쿠키 전송 여부를 결정함. `Lax`나 `Strict` 설정을 통해 CSRF 공격을 효과적으로 방어

### 1.2 서버 측 세션 관리와 확장성 이슈

세션은 민감 정보를 서버(메모리, DB 등)에 두고 클라이언트에게는 **Session ID**만 전달하는 방식

- **장점:** 서버가 세션 생명주기를 완벽히 통제할 수 있어 보안 사고 시 즉각적인 로그아웃 처리가 가능
- **단점:** 사용자가 늘어날수록 서버 메모리 부하가 가중됨. 다중 서버 환경에서는 세션 정보가 공유되지 않음
- **대안:** Redis와 같은 외부 캐시 시스템을 활용하나, 매 요청마다 발생하는 네트워크 I/O가 지연 시간의 원인이 됨

## 2. JWT

JWT는 토큰 자체가 정보를 담고 있는 **Self-contained** 구조로 서버가 상태를 저장할 필요가 없어 확장성이 매우 뛰어남

### 2.1 JWT의 3단 구조

1. **Header :** 사용된 해시 알고리즘(예: HS256)과 토큰 타입(JWT) 정보를 담고 있음
2. **Payload :** 실제 전송할 데이터인 클레임을 포함
    - **Registered :** `iss`(발행자), `exp`(만료), `sub`(식별자) 등 표준 규격.
    - **Public/Private:** 협의된 사용자 정의 정보.
    - **주의 :** Base64Url로 인코딩만 되어 있으므로 누구나 복호화가 가능하므로 절대 민감 정보를 담으면 안 됨
3. **Signature :** 헤더와 페이로드를 비밀 키로 해싱하여 생성하므로 토큰의 무결성을 보장

### 2.2 서명 알고리즘: 대칭키 vs 비대칭키

- **HS256 (HMAC):** 동일한 비밀 키로 서명하고 검증함. 구조가 단순하고 빠르나, 키 유출 시 전체 보안이 무너지는 리스크 존재
- **RS256 (RSA):** 개인 키로 서명하고 공개 키로 검증함. 검증 주체가 공개 키만 가지면 되므로 마이크로서비스(MSA) 환경에서 서비스 간 독립적인 검증이 가능

## 3. 토큰 생명주기 : 액세스 및 리프레시 토큰

JWT의 최대 단점인 '한번 발행하면 무효화가 어렵다'는 점을 해결하기 위해 두 종류의 토큰을 조합할 수 있음

### 3.1 토큰의 역할 분리

- **Access Token :** 실제 API 호출에 사용되며 유효 기간을 짧게 설정함. 탈취되어도 피해 범위를 시간적으로 제한할 수 있음
- **Refresh Token :** 새로운 액세스 토큰을 발급받기 위한 용도임. 유효 기간을 길게 설정하며, 노출 빈도를 낮추기 위해 전용 엔드포인트에서만 사용

### 3.2 리프레시 토큰 회전(RTR) 및 재사용 탐지

- **RTR :** 새로운 액세스 토큰 발급 시 리프레시 토큰도 매번 새로 교체해주는 방식
- **재사용 탐지 :** 공격자가 탈취한 예전 리프레시 토큰을 사용하려 할 경우, 서버는 이를 '비정상적 재사용'으로 간주함. 해당 사용자의 모든 토큰을 즉시 무효화하여 추가 피해를 차단

## 4. 클라이언트 저장소 보안 전략

### 4.1 LocalStorage vs HttpOnly 쿠키

- **LocalStorage :** 사용은 편하나 XSS 공격에 무방비함. 스크립트 한 줄로 토큰을 전부 털릴 수 있음.
- **HttpOnly 쿠키 :** XSS에는 강하나 CSRF 공격에 취약함. 브라우저가 자동으로 쿠키를 태워 보내기 때문

### 4.2 가장 권장되는 접근법

1. **Access Token :** 자바스크립트의 변수나 클로저(In-Memory)에 저장함. 페이지를 새로고침하면 사라지지만 XSS로부터 가장 안전
2. **Refresh Token:** `HttpOnly`, `Secure`, `SameSite` 속성이 적용된 쿠키에 저장
3. **동작:** 새로고침 시 쿠키의 리프레시 토큰으로 서버에 요청하여 액세스 토큰을 다시 받아 메모리에 올림

## 5. OAuth 2.0과 OpenID Connect (OIDC)

로그인 인증과 권한 위임을 표준화한 프레임워크

### 5.1 OAuth 2.0과 PKCE

- **핵심 :** 비밀번호 노출 없이 제3자 앱에 특정 자원 접근 권한을 부여
- **PKCE (Proof Key for Code Exchange) :** 클라이언트 보안이 취약한 환경에서 필수로 코드 탈취 후 토큰 교환을 시도해도 미리 전달한 해시값과 검증용 난수가 맞지 않으면 토큰 발급을 거부

### 5.2 OIDC (OpenID Connect)

- **목적:** OAuth 2.0 기반 위에서 '사용자가 누구인지(Identity)' 확인하는 계층을 추가
- **ID Token :** JWT 형식의 신원 증명서임. 클라이언트는 이를 파싱하여 별도의 API 호출 없이도 사용자 프로필 정보를 즉시 표시할 수 있음

## 6. 소셜 로그인의 원리와 작동 과정

### 6.1 주요 참여자

<img width="820" height="396" alt="image" src="https://github.com/user-attachments/assets/13650652-9426-44e9-a6e3-961d27d290de" />


- **자원 소유자 (Resource Owner) :** 소셜 계정을 가진 실제 사용자
- **인가 서버 (Authorization Server) :** 사용자를 인증하고 토큰을 발급하는 소셜 서비스의 서버
- **리소스 서버 (Resource Server) :** 사용자의 이름, 이메일 등 프로필 정보를 가진 소셜 서비스의 API 서버

### 6.2 인증 코드 승인 방식의 단계별 과정

가장 보안성이 높아 널리 쓰이는 방식이며, 클라이언트 사이드(브라우저)에 토큰이 직접 노출되는 것을 방지

1. **로그인 시도 :** 사용자가 서비스의 '구글로 로그인' 버튼을 클릭
2. **인가 요청 (Redirect) :** 서비스가 사용자를 소셜 서비스의 로그인 페이지로 보냄. 이때 `client_id`, `redirect_uri`, `scope`(가져올 정보 범위)를 함께 전달
3. **인증 및 동의 :** 사용자가 소셜 서비스에 로그인하고, 서비스가 요청한 정보 제공에 동의함.
4. **인가 코드 발급 :** 소셜 서비스가 사용자를 서비스의 `redirect_uri`로 다시 보내면서, 일회용 인가 코드를 전달
5. **토큰 교환 :** 서비스의 백엔드 서버가 받은 인가 코드를 가지고 소셜 서버에 직접 접속함. 코드를 주고 실제 액세스 토큰과 ID 토큰을 발급받음
6. **사용자 정보 획득:** 서비스 백엔드가 액세스 토큰을 사용하여 소셜 리소스 서버로부터 사용자의 프로필 정보를 가져옴
7. **로그인 완료:** 가져온 정보를 바탕으로 서비스 자체의 세션이나 JWT를 생성하여 사용자에게 전달함으로써 로그인을 마무리

### 6.3 왜 인가 코드를 먼저 받는가?

- **보안 강화:** 브라우저(프론트엔드)는 주소창이나 개발자 도구를 통해 데이터가 노출될 위험이 큼
- **서버 간 통신:** 실제 중요한 토큰(Access Token)은 백엔드 서버와 소셜 서버 간의 안전한 통신을 통해서만 오가게 하여 탈취 위험을 원천 차단하기 위함

## 7. OIDC와 ID 토큰의 역할

과거 OAuth 2.0은 '권한 위임'만 가능했으나, 그 위에 **OIDC** 계층이 생기면서 신원 확인이 쉬워짐

| **구분** | **OAuth 2.0 (Access Token)** | **OIDC (ID Token)** |
| --- | --- | --- |
| **핵심 목적** | 리소스 서버의 API 호출 권한 획득 | 사용자가 누구인지 증명 (신원 확인) |
| **데이터 형태** | 보통 불투명한 문자열 (Opaque) | 표준화된 **JWT** 형식 |
| **주요 내용** | Scope(권한 범위), 만료 시간 | 유저 ID, 이메일, 프로필 사진 등 |
- 소셜 로그인 시 발급되는 **ID Token**은 그 자체로 유효성이 검증된 JWT이므로, 서비스 백엔드는 별도의 API 호출 없이도 토큰 내부의 정보를 믿고 즉시 회원가입이나 로그인을 처리할 수 있음

## 8. 소셜 로그인의 보안적 이점

- **비밀번호 관리 부담 해소 :** 서비스가 사용자의 비밀번호를 직접 저장하지 않으므로 DB 유출 시에도 비밀번호 보안 사고에서 자유로움
- **신뢰도 높은 인증 :** 구글이나 카카오 같은 대형 플랫폼의 강력한 보안 시스템(2단계 인증 등)을 그대로 활용할 수 있음
- **간편한 사용자 경험 :** 사용자는 별도의 가입 절차 없이 클릭 몇 번으로 서비스를 시작할 수 있음

</details>

<details><summary><h1>JWT 토큰 및 권한 테스트</h1></summary>

현재 내 Security Config에 설정된 권한은 다음과 같다
```java
 http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/api/v1/auth/**").permitAll() // /api/v1/auth/ 하위 경로는 모두 허용
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // Swagger 허용
                .requestMatchers("/", "/health-check").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN") // admin 경로 권한설정
                .requestMatchers(HttpMethod.GET,"/api/reviews/").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/movies/").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/reviews/movie/").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/schedules/").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/theaters/").permitAll()
                .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
```

- 회원가입 API
  
  <img width="702" height="508" alt="image" src="https://github.com/user-attachments/assets/bc389b1e-ba5e-4bb3-9d5b-cac88dcd8b57" />
  
- 로그인 API
 <img width="1332" height="502" alt="image" src="https://github.com/user-attachments/assets/da1a39ba-5dad-42f3-a654-656fa226ef59" />

Access token과 refresh token을 발급

- JWT 토큰 적용
  
  <img width="685" height="292" alt="image" src="https://github.com/user-attachments/assets/c29ae10d-89ee-4ac7-9aee-f76b225c711b" />

- 리프레쉬 토큰으로 재발급

  <img width="1096" height="354" alt="image" src="https://github.com/user-attachments/assets/63a14577-3460-496f-be61-f2ead2690cc2" />

  
- 로그인된 사용자만 사용할 수 있는 API
  
  <img width="1102" height="343" alt="image" src="https://github.com/user-attachments/assets/0fd9480d-7edb-4681-b67b-5f4f34b2187b" />
  
- 로그인된 사용자만 사용할 수 있는 API에 로그인 안 한 유저 접근하려 할 때
  
  <img width="686" height="398" alt="image" src="https://github.com/user-attachments/assets/cd50baf4-2f4e-435f-8879-cab58efc41da" />

  401 에러를 반환
  
- 관리자만 사용할 수 있는 API (/api/admin/**)
  
  <img width="952" height="452" alt="image" src="https://github.com/user-attachments/assets/9abb9869-9c8c-43be-b173-a90268432129" />

- 관리자만 사용할 수 있는 API에 일반 유저가 접근하려 할 때
  
  <img width="812" height="459" alt="image" src="https://github.com/user-attachments/assets/1912f40e-74a6-4aed-9b9f-291edf5feef8" />

  JWT 토큰을 가지고 있지만 role이 user 이므로 403 에러 발생


</details>

<details><summary><h1>401, 403 에러의 공통 응답 미적용 이유와 해결 방안</h1></summary>

### 1. 공통 응답 형식이 적용되지 않는 이유: 발생 시점의 차이
* **필터와 인터셉터의 위치 차이**: Spring Security는 서블릿 필터 기반으로 동작하며, 이는 스프링 MVC의 `DispatcherServlet`보다 앞단에 위치
* **@RestControllerAdvice의 한계**: 현재 프로젝트에서 사용 중인 `ExceptionAdvice`는 `@RestControllerAdvice`를 사용함. 이는 Controller 이후 영역(Interceptor, Controller, Service 등)에서 발생하는 예외만 가로챌 수 있음
* **보안 필터의 예외 발생**: 401(인증 실패)과 403(권한 부족) 에러는 `DispatcherServlet`에 도달하기 전인 보안 필터 체인(Security Filter Chain)에서 발생함. 따라서 `ExceptionAdvice`가 이를 인지하지 못하고 서블릿의 기본 에러 응답이 출력되는 것

### 2. 프로젝트에서의 해결 방식: 커스텀 핸들러 도입
스프링 시큐리티의 진입점과 권한 핸들러를 직접 구현하여 이 문제를 해결함

#### ① 401 에러 해결: CustomAuthenticationEntryPoint
* **인증되지 않은 접근 제어**: 로그인이 필요한 API에 토큰 없이 접근할 때 발생하는 예외를 처리
* **직접 응답 바디 작성**: `commence` 메서드 내에서 `ObjectMapper`를 사용하여 `ApiResponse` 객체를 직접 JSON 문자열로 변환한 뒤, `HttpServletResponse`의 출력 스트림에 직접 써주는 방식
* **응답 규격 통일**: 이를 통해 인증 실패 시에도 `isSuccess`, `code`, `message` 등을 포함한 공통 포맷이 반환되도록 보장

#### ② 403 에러 해결: CustomAccessDeniedHandler
* **권한 부족 접근 제어**: 인증은 되었으나 관리자 페이지에 일반 유저가 접근하는 경우 등을 처리
* **핸들러 구현**: `AccessDeniedHandler` 인터페이스를 구현하여 `ApiResponse.onFailure`를 통해 에러 응답을 생성하고, 403 상태 코드와 함께 JSON을 반환

#### ③ SecurityConfig 등록
* **필터 체인 적용**: 구현한 두 커스텀 객체를 `SecurityConfig`의 `exceptionHandling` 설정에 등록하여 보안 필터 과정에서 해당 핸들러들이 동작하도록 지정

</details>

<details><summary><h1>동시성 · 결제 시스템 연동</h1></summary>


## 1. 코드 리팩토링: Rich Domain Model 관점에서의 개선

### 1-1. Rich Domain Model이란

Rich Domain Model은 **데이터와 그 데이터를 다루는 핵심 비즈니스 규칙을 같은 객체 안에 함께 두는 방식**이다.
 : 엔티티가 단순히 필드만 들고 있는 자료 구조가 아니라, 자신의 상태를 어떻게 바꿀 수 있는지,어떤 전이가 허용되는지, 어떤 값이 유효한지를 스스로 책임지는 모델이다.

즉, 서비스 계층은 "무엇을 시킬지"를 조율하고, 도메인 객체는 "어떻게 상태를 바꿀지"를 책임진다.

### 1-2. 이 방식이 OOP를 준수하는 이유

- **캡슐화**: 상태와 행위를 한 객체 안에 둠으로써 외부에서 잘못된 상태 변경을 직접 하지 못하게 막을 수 있다.
- **책임 분리**: 서비스는 트랜잭션과 외부 시스템 연동을 담당하고, 엔티티는 자신의 규칙과 상태 전이를 담당한다.
- **응집도 향상**: 해당 도메인과 관련된 규칙이 엔티티에 모여 있어 읽기 쉽고 변경 지점이 명확하다.
- **절차지향 코드 감소**: 서비스가 `if-else`로 상태를 일일이 바꾸는 대신 엔티티 메서드를 호출하도록 바뀌어 코드 의도가 분명해진다.

### 1-3. 기존 코드에서 Poor Domain Model이었던 부분

초기 구조에서는 일부 엔티티가 사실상 데이터 보관 역할에 가까웠고, 핵심 비즈니스 규칙이 서비스에 분산되어 있었다.

- 생성 책임이 서비스에 퍼져 있었음
- 상태 전이 검증이 서비스에서 처리되거나 아예 비어 있는 경우가 있었음
- 같은 종류의 검증 로직이 여러 서비스에 중복되었음
- 서비스 메서드가 한 번에 너무 많은 일을 하며 절차적으로 길어지는 경향이 있었음

대표적으로 `FoodOrder`는 이전에 `confirm()` 과 `cancel()`이 상태 가드 없이 값을 바꾸는 구조였기 때문에, 도메인 객체가 스스로 잘못된 전이를 막지 못했다.

### 1-4. 어떻게 개선했는가

#### ① 생성 책임을 도메인으로 이동

엔티티 생성 시점의 규칙을 서비스가 직접 조립하지 않고 도메인 정적 팩토리 메서드로 이동했다.

예시:

```java
public static Reservation create(User user, MovieScreen movieScreen, LocalDateTime now) {
    if (movieScreen.getStartAt().isBefore(now)) {
        throw new GeneralException(GeneralErrorCode.MOVIE_ALREADY_STARTED);
    }

    return Reservation.builder()
            .user(user)
            .movieScreen(movieScreen)
            .status(ReservationStatus.대기)
            .totalPrice(0)
            .paymentId(null)
            .reservationSeats(new ArrayList<>())
            .build();
}
```

이렇게 하면 "예매 생성 시 이미 시작한 영화는 생성할 수 없다"는 규칙이 `Reservation` 안에 응집된다.

#### ② 상태 전이 규칙을 엔티티에 배치

`Reservation`은 `assignPaymentId()`, `confirm()`, `cancel()` 안에서 자신의 상태를 검증하고 직접 전이한다.

```java
public void confirm() {
    if (this.status != ReservationStatus.대기) {
        throw new GeneralException(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    if (this.paymentId == null || this.paymentId.isBlank()) {
        throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY);
    }

    this.status = ReservationStatus.완료;
}
```

매점 주문도 같은 방향으로 리팩토링하여 `FoodOrder`가 스스로 유효한 상태 전이만 허용하도록 개선했다.

#### ③ 서비스 메서드를 private helper method로 분리

서비스 계층의 긴 메서드를 역할별 private 메서드로 나누어 가독성을 높였다.

예를 들어 `ReservationService`는 다음처럼 분리되었다.

- `getUser`
- `getMovieScreen`
- `validateSeatRequest`
- `processSeatReservations`
- `validateOwner`
- `toReservationResponse`

이 방식으로 바꾼 이유는 다음과 같다.

- 하나의 메서드가 여러 레벨의 추상화를 섞지 않게 하기 위해
- 예외 처리와 핵심 흐름을 구분하기 위해
- 중복 코드를 제거하고 수정 지점을 명확히 하기 위해
- 테스트/리뷰 시 메서드 이름만 보고도 의도를 파악할 수 있게 하기 위해

즉, 기존 절차지향적 서비스 코드가 "한 메서드 안에서 순서대로 다 처리"하던 구조였다면, 리팩토링 후에는 "핵심 흐름 + 의미 있는 helper 메서드 조합" 구조로 읽히도록 개선했다.

### 1-5. 리팩토링 후 역할 분리

- **Controller**: 요청/응답, 인증 사용자 추출
- **Facade / Service**: 트랜잭션 경계, 외부 시스템 연동, 유스케이스 조율
- **Entity**: 상태 전이, 유효성 검증, 생성 규칙
- **Repository**: 조회 전략과 락 전략

이 구조로 인해 서비스는 "절차"보다 "시나리오 조율"에 집중하고, 도메인 객체는 자신의 규칙을 직접 가지게 되었다.

---

## 2. 동시성 해결 방법 조사 및 프로젝트 적용

### 2-1. 동시성 제어 방식 비교

#### ① `synchronized`

- **장점**: 자바 코드 레벨에서 가장 단순하게 적용 가능
- **단점**: 단일 JVM 안에서만 유효하며, 서버가 여러 대인 환경에서는 동시성 제어가 불가능
- **적합한 경우**: 단일 서버, 단순 임계 구역 보호
- **우리 서비스에서 부적합한 이유**: 영화 예매/결제/재고 차감은 다중 서버에서도 동일한 무결성을 보장해야 하므로 JVM 내부 락만으로는 부족함

#### ② DB Lock

##### Pessimistic Lock

- **장점**: 충돌이 자주 나는 자원에 대해 강하게 무결성을 보장함
- **단점**: 대기 시간이 길어질 수 있고, 데드락 위험이 있음
- **적합한 경우**: 재고 차감, 좌석 선점처럼 충돌 시 비용이 큰 경우

##### Optimistic Lock

- **장점**: 읽기 위주의 환경에서 성능상 유리하고 DB 락을 오래 잡지 않음
- **단점**: 충돌 발생 시 재시도 로직이 필요하며, 충돌 빈도가 높으면 오히려 불편
- **적합한 경우**: 조회가 많고 동시에 수정될 가능성이 상대적으로 낮은 경우

##### Named Lock

- **장점**: 실제 레코드가 없더라도 문자열 키 기준으로 락을 걸 수 있음
- **단점**: DB 의존도가 높고 구현/운영 복잡도가 증가
- **적합한 경우**: 아직 생성되지 않은 자원에 대해 선점 제어가 필요한 경우

#### ③ Redis 기반 분산 락

##### Lettuce

- **장점**: 구현이 비교적 단순하고 가벼움
- **단점**: 재시도/타임아웃/락 해제 안전성 등을 직접 더 많이 관리해야 함
- **적합한 경우**: 간단한 분산 락

##### Redisson

- **장점**: 재진입 락, 대기, lease time, watchdog 등 분산 락 기능이 풍부함
- **단점**: Redis 인프라와 운영 부담이 추가됨
- **적합한 경우**: 다중 서버에서 분산 락을 본격적으로 운영해야 하는 경우

### 2-2. CGV 서비스에서 어떤 방식이 적합한가

현재 프로젝트의 핵심 충돌 자원은 다음과 같다.

- 같은 상영 회차의 같은 좌석
- 같은 극장의 같은 음식 재고
- 같은 상영관의 시간표 등록

이 자원들은 모두 정합성이 매우 중요하지만, 자원의 성격은 서로 다르다.

- **예매 좌석**: 실제로 보호해야 하는 대상은 `Seat` 행 자체가 아니라 `movieScreenId + seatId` 조합이다.
- **음식 재고 / 상영관 시간표**: 이미 존재하는 재고 row, 상영관 row를 기준으로 보호하면 된다.

따라서 현재 프로젝트에서는 자원 특성에 따라 락 전략을 구분했다.

- **예매 좌석**: `Named Lock + 유니크 제약 + 도메인 검증`
- **매점 재고 / 상영 시간표**: `Pessimistic Lock + 도메인 검증`

이 방식을 선택한 이유는 다음과 같다.

- 좌석 예매는 `같은 회차의 같은 좌석`만 정확히 직렬화하면 됨
- 재고 차감과 시간표 등록은 이미 존재하는 부모 row를 잠그는 방식이 단순하고 명확함
- 중복 예매나 음수 재고는 절대 허용되면 안 됨
- 단순 조회보다 정합성이 더 중요함
- Redis 분산 락을 붙이기 전에도 DB 수준에서 확실한 보호 장치가 필요함

Q : 같은 상영관의 시간표 등록 충돌은 잘 안 일어날 것 같은데 왜 비관적락으로 설정했나요?
A :
그 이유는 이 기능의 핵심이 **기존 데이터 수정 충돌 감지**보다 **동일 상영관에 대한 중복 시간표 등록 방지**에 있기 때문이다.

스케줄 등록 로직은 다음 순서로 동작한다.

1. 특정 상영관(`Screen`)을 조회하면서 락을 획득한다.
2. 해당 상영관에 이미 겹치는 시간대의 스케줄이 존재하는지 검사한다.
3. 겹치지 않으면 새로운 스케줄(`MovieScreen`)을 저장한다.

이 과정에서 여러 요청이 동시에 들어오면, 둘 다 아직 스케줄이 없다고 판단한 뒤 동시에 insert를 수행하는 **레이스 컨디션**이 발생할 수 있다.  
특히 이 문제는 “같은 row를 동시에 수정”하는 상황이 아니라, **조건을 확인한 뒤 새로운 row를 추가하는 상황**이기 때문에 단순한 `@Version` 기반 낙관적 락만으로는 안전하게 막기 어렵다.

### 왜 낙관적 락이 아닌가?

낙관적 락은 보통 이미 존재하는 엔티티에 `@Version` 필드를 두고, **수정 시점에 버전 충돌을 감지**하는 방식이다.  
하지만 영화 스케줄 등록은 주로 `MovieScreen`을 새로 생성하는 작업이므로, 충돌 시점이 “기존 row 수정”이 아니라 “동시 insert”에 가깝다.

즉, 다음과 같은 문제가 있다.

- 두 요청이 동시에 상영관의 기존 스케줄을 조회할 수 있다.
- 둘 다 겹치는 스케줄이 없다고 판단할 수 있다.
- 그 뒤 각각 새로운 스케줄을 저장하면 중복 등록이 발생할 수 있다.

이 경우 단순히 `MovieScreen`에 `@Version`을 두는 것만으로는 충돌을 막기 어렵다.  
충돌을 안전하게 막으려면 별도의 버전 증가 전략, 강제 갱신, 혹은 더 복잡한 DB 제약 설계가 추가로 필요하다.

### 왜 비관적 락이 적절한가?

비관적 락은 상영관(`Screen`) 조회 시점에 DB 레벨에서 락을 먼저 획득하므로,  
**같은 상영관에 대한 스케줄 등록 작업을 동시에 하나만 진행하도록 보장**할 수 있다.

이 방식의 장점은 다음과 같다.

- 시간표 겹침 검사와 저장을 하나의 보호 구간으로 묶을 수 있다.
- 동시 요청 상황에서도 중복 등록을 안정적으로 방지할 수 있다.
- 구현이 비교적 단순하고, 동작 의도가 명확하다.

즉, 이 기능에서는 성능 최적화보다 **상영 시간표 무결성 보장**이 더 중요하므로 비관적 락을 선택했다.

### 2-3. 실제 코드 적용 방식

#### 예매 좌석

- `movieScreenId + seatId` 조합을 문자열 키로 만들어 `Named Lock`을 획득
- `ReservationSeat` 에 `UNIQUE (movie_screen_id, seat_id)` 제약을 둠
- `ReservationStatus.대기`, `완료` 상태를 모두 점유 상태로 간주하여 중복 선택을 막음

예매 좌석은 물리 좌석(`Seat`) 자체를 잠그는 것이 아니라, **특정 상영 회차에서의 특정 좌석**을 잠그는 것이 핵심이다.

만약 `Seat` row 자체에 비관적 락을 걸면, 같은 물리 좌석이라도 서로 다른 `movieScreenId` 예매까지 함께 대기하게 된다.  
예를 들어 `A1` 좌석이 10시 회차와 14시 회차에 모두 존재할 때, 우리가 막아야 하는 충돌은 `10시 A1 vs 10시 A1`이지 `10시 A1 vs 14시 A1`이 아니다.

그래서 예매에서는 다음과 같이 `reservation:{movieScreenId}:{seatId}` 형식의 키로 네임드 락을 사용한다.

```java
List<String> lockKeys = seatIds.stream()
        .sorted()
        .map(seatId -> "reservation:" + movieScreenId + ":" + seatId)
        .toList();

reservationNamedLockManager.acquireLocks(lockKeys);
```

이 방식을 선택한 이유는 다음과 같다.

- 예매 충돌의 기준이 `seat_id` 단독이 아니라 `movieScreenId + seatId` 조합이기 때문
- 아직 `ReservationSeat` row가 생성되기 전에도 문자열 키만으로 선점 제어가 가능하기 때문
- 서로 다른 회차의 같은 물리 좌석은 동시에 처리할 수 있어 락 범위를 더 정확히 줄일 수 있기 때문
- `ReservationSeat`의 유니크 제약이 마지막 안전망 역할을 계속 수행하기 때문

```java
boolean isAlreadyReserved = reservationSeatRepository
        .existsByMovieScreenIdAndSeatIdAndReservation_StatusIn(
                movieScreenId, seatId, List.of(ReservationStatus.완료, ReservationStatus.대기)
        );
```

#### 대기 상태 좌석 유지

예매 생성 시 상태는 `대기`로 생성되고, 이 상태도 점유로 처리된다.

```java
return Reservation.builder()
        .user(user)
        .movieScreen(movieScreen)
        .status(ReservationStatus.대기)
        .totalPrice(0)
        .build();
```

이후 5분이 지난 대기 예매는 스케줄러와 bulk update로 만료 처리한다.

```java
@Scheduled(fixedDelay = 60000)
public void expirePendingOrders() {
    pendingOrderExpirationService.expirePendingReservationsAndFoodOrders();
}
```

즉, 대기 상태에서는 다른 사용자가 같은 좌석을 선택할 수 없고 만료 후에는 좌석 점유가 해제되어 다시 선택 가능하다

이 부분은 `PendingReservationHoldIntegrationTest`로 검증했다.

#### 매점 재고

매점 주문은 **주문 생성 시 재고를 홀드하지 않고**, **결제 성공 후에만 차감**하는 정책을 적용했다.

```java
TheaterFood theaterFood = theaterFoodRepository
        .findByTheaterAndFoodWithLock(foodOrder.getTheater(), item.getFood())
        .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

theaterFood.decreaseStock(item.getQuantity());
```

이 정책을 택한 이유는 다음과 같다.

- 과제 요구사항이 "결제가 진행된 후 재고가 차감되는지"에 초점이 있었음
- 주문 생성 시점에 락을 오래 잡으면 오히려 락 경쟁이 커질 수 있음
- 최종 완료 수량만 정확하면 오버셀링은 방지 가능함

즉, 주문은 여러 개 생성될 수 있어도 실제 결제 성공 후 차감 단계에서 락을 잡고 순차 차감하므로 최종적으로 재고보다 많이 판매되는 오버셀링은 막을 수 있다

이 부분은 `FoodPaymentFlowIntegrationTest`로 검증했다.

### 2-4. 이번 프로젝트에서 정리한 결론

- **단일 서버 키워드인 `synchronized`는 부적합**
- **현재 자원 특성에는 Pessimistic Lock이 가장 적합**
- **존재하지 않는 row에 대한 락이 필요하면 Named Lock 또는 분산 락 고려 가능**
- **다중 서버 확장과 고도화가 필요해지면 Redis Redisson 도입을 검토할 수 있음**

---

## 3. 결제 시스템 연동 및 취소 로직

### 3-1. 적용한 결제 시나리오

이번 과제에서는 영화 예매와 매점 주문 모두 외부 결제 서버와 연동했다.

- 영화 예매: 예매 생성 후 결제 성공 시 `대기 -> 완료`
- 영화 예매 취소: 완료된 예매는 외부 결제를 먼저 취소한 뒤 내부 예매를 취소
- 매점 주문: 결제 성공 시 재고 차감
- 매점 주문 실패/재고 부족: 외부 결제 취소 또는 내부 보상 취소 처리

### 3-2. Feign Client를 선택한 이유

처음에는 `RestTemplate` 기반 설정이 있었지만, 외부 결제 서버 연동 방식은 `Feign Client`로 변경했다.

적용 코드:

```java
@FeignClient(
        name = "paymentClient",
        url = "${payment.server.url}",
        configuration = PaymentFeignConfig.class
)
public interface PaymentFeignClient {

    @PostMapping(value = "/payments/{paymentId}/instant", consumes = "application/json")
    PaymentResponse requestInstantPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody InstantPaymentRequest request
    );

    @PostMapping("/payments/{paymentId}/cancel")
    PaymentResponse cancelPayment(@PathVariable("paymentId") String paymentId);

    @GetMapping("/payments/{paymentId}")
    PaymentResponse getPayment(@PathVariable("paymentId") String paymentId);
}
```

#### Feign을 선택한 이유

- **선언형 인터페이스 기반**이라 코드가 짧고 읽기 쉽다
- 결제 API 명세가 곧 자바 인터페이스 형태로 드러나서 유지보수성이 높다
- 헤더 주입, 예외 변환, 설정 분리가 쉽다
- 외부 API 클라이언트라는 의도가 코드에서 명확하게 드러난다

#### `RestTemplate` / 일반 HTTP Client와 비교

##### RestTemplate

- **장점**: 단순 호출에는 익숙하고 직관적
- **단점**: URL, 헤더, 요청/응답 매핑 코드가 서비스 안에 섞이기 쉬움
- **결론**: 외부 결제 API처럼 엔드포인트가 여러 개일 때 선언형 인터페이스보다 장황해지기 쉽다

##### Java HttpClient / WebClient

- **장점**: 유연성이 높고 비동기 처리까지 가능
- **단점**: 현재 프로젝트에서는 오히려 설정량이 많고 보일러플레이트가 늘어남

##### Feign

- **장점**: API 명세가 메서드로 바로 표현됨
- **장점**: 관심사 분리가 좋고 테스트 작성도 수월함
- **장점**: 결제 서버처럼 "정해진 외부 API를 호출하는 역할"에 적합함

즉, 이번 프로젝트에서는 **복잡한 커스텀 HTTP 처리보다, 결제 API를 명확한 클라이언트 인터페이스로 표현하는 것이 더 큰 이점**이 있다고 판단하여 Feign을 선택했다.

### 3-3. 인증 헤더 및 설정 분리

결제 서버 호출에 필요한 secret은 Feign 설정으로 분리했다.

```java
@Bean
public RequestInterceptor paymentRequestInterceptor(PaymentProperties paymentProperties) {
    return requestTemplate -> requestTemplate.header(
            HttpHeaders.AUTHORIZATION,
            "Bearer " + paymentProperties.apiSecret()
    );
}
```

이를 통해 서비스 로직 안에 헤더 설정 코드가 섞이지 않도록 분리했다.

### 3-4. 결제 실패 시 보상 트랜잭션

결제/재고/예약 확정 흐름에서는 외부 결제와 내부 상태 변경이 함께 일어나므로, 실패 시 보상 처리 설계가 중요했다.

처음에는 실패 시 같은 트랜잭션 안에서 `cancel()`을 호출하는 방식이었지만, 예외가 다시 던져지면 롤백되면서 보상 취소까지 함께 롤백되는 문제가 있었다.

이를 해결하기 위해 `PaymentCompensationService`를 분리하고 `REQUIRES_NEW` 전파 속성으로 보상 로직을 별도 트랜잭션에서 수행하도록 변경했다.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void cancelFoodOrder(Long orderId) {
    FoodOrder foodOrder = foodOrderRepository.findById(orderId)
            .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));
    foodOrder.markPaymentCancelled();
    foodOrder.cancel();
}
```

이 구조의 핵심은 "메인 트랜잭션이 롤백되더라도, 결제 취소 이후 내부 상태 보정은 별도 트랜잭션에서 안전하게 커밋한다"는 점이다.
다만 메인 트랜잭션 자체에는 `noRollbackFor`를 두지 않았다.
재고 차감이나 좌석 확정처럼 여러 자원을 순차적으로 갱신하는 로직은 예외 발생 시 전체가 함께 롤백되어야 데이터 정합성을 보장할 수 있기 때문이다.

**트랜잭션 전파 속성 (Propagation.REQUIRES_NEW)**
* **REQUIRES_NEW** : 기존에 실행 중인 트랜잭션의 존재 여부와 상관없이, 항상 새로운 독립적인 트랜잭션을 생성하여 실행하는 설정
* **동작 방식** : 기존 메인 로직을 처리하던 부모 트랜잭션이 실행 중일 때 호출되면, 부모 트랜잭션을 잠시 대기(Suspend) 상태로 만듦.
  이후 새로운 자식 트랜잭션을 시작하여 독립적으로 커밋(Commit) 또는 롤백(Rollback)을 수행함. 자식 트랜잭션 처리가 끝나면 대기 중이던 부모 트랜잭션이 다시 재개됨.
* **핵심 특징** : 부모 트랜잭션과 자식 트랜잭션은 물리적으로 분리된 커넥션을 사용하므로, 부모 트랜잭션에 예외가 발생해 롤백되더라도 자식 트랜잭션의 커밋 결과에는 영향을 주지 않음.

**보상 로직(PaymentCompensationService)에 REQUIRES_NEW를 도입한 이유**
* **기본 설정(REQUIRED)의 한계** : 결제나 예외가 발생하면 기존 트랜잭션은 이미 '롤백 전용(Rollback-only)' 상태로 마킹됨. 이 상태에서 동일한 트랜잭션 내에 있는 `foodOrder.cancel()`을 호출해 주문 상태를 '취소'로 바꾸려 해도, 최종적으로 전체 트랜잭션이 롤백되므로 DB에는 취소 상태가 반영되지 않고 예전 상태로 돌아가 버림.
* **REQUIRES_NEW 적용의 효과** : 메인 비즈니스 로직(부모 트랜잭션)이 예외로 인해 파기되더라도, 주문 취소 로직은 완전히 독립된 새로운 트랜잭션에서 실행됨. 따라서 부모 트랜잭션의 롤백 여부와 무관하게 "주문 상태를 CANCELED로 변경하는 작업"만큼은 데이터베이스에 안전하고 확실하게 커밋됨.

**각 실패 상황별 동작 흐름**
* **외부 결제 실패** : PG사 응답 자체가 실패로 확정된 경우에는 보상 트랜잭션으로 취소하지 않고 `paymentStatus=FAILED`로 남긴다.
* **외부 서버 오류/타임아웃** : 결제 결과를 확정할 수 없는 경우에는 `paymentStatus=UNKNOWN`으로 남겨 후속 확인이 가능하도록 한다.
* **재고 차감 실패 / 좌석 확정 실패** : 외부 결제는 성공했지만 내부 후속 처리에 실패한 경우에만 보상 트랜잭션을 실행해 `status=취소`, `paymentStatus=CANCELLED`로 정리한다.
* **권한 실패 / 주문자 검증 실패** : 보상 취소 대상으로 보지 않고 예외만 반환한다. 아직 유효한 주문이나 예매를 임의로 `취소` 상태로 확정하면 도메인 의미가 흐려질 수 있기 때문이다.

### 3-5. 주문자 검증 추가

매점 결제는 초기에 `orderId`만으로 결제가 가능했기 때문에 주문 소유자 검증이 부족했다.
이를 개선하여 Controller에서 `userId`를 받고 Service에서 `getOwnedFoodOrderWithLock`으로 주문자 검증을 수행하고 본인 주문일 때만 결제가 진행되도록 변경했다.
이는 예매 결제와 동일한 수준의 권한 검증을 맞추기 위한 리팩토링이다.

### 3-6. 외부 결제 호출과 DB 트랜잭션 분리

초기 `FoodPaymentFacade`는 `@Transactional` 범위 안에서 외부 결제 API를 먼저 호출하고, 그 뒤 재고 차감과 주문 확정을 처리하고 있었다.

이 방식은 정합성보다 먼저 **커넥션 풀 고갈 위험**을 만들 수 있었다.

- 트랜잭션 시작과 동시에 DB 커넥션을 점유함
- 외부 결제 서버 응답이 느리면 그동안 커넥션이 반환되지 않음
- 동시 요청이 몰리면 결제와 무관한 조회/쓰기까지 같이 막힐 수 있음

이를 줄이기 위해 매점 결제 흐름은 다음처럼 조정했다.

- 주문 조회는 짧은 읽기 트랜잭션으로 수행
- 외부 결제 API 호출은 트랜잭션 밖에서 수행
- 결제 성공 후에만 별도의 짧은 로컬 트랜잭션에서 재고 차감과 주문 확정 수행

즉, 외부 네트워크 대기 시간 동안 DB 커넥션을 오래 붙잡지 않도록 트랜잭션 경계를 분리했다.

### 3-7. 주문 상태와 결제 상태 분리

기존에는 `대기 / 완료 / 취소` 하나의 상태값으로 주문/예매의 생명주기와 결제 결과를 모두 표현하고 있었다.

이 구조에서는 다음 문제가 있었다.

- 실제 결제 실패와 사용자 취소가 모두 `취소`로 보임
- 외부 결제 서버 장애나 타임아웃처럼 결제 결과가 불명확한 상황도 `취소`로 처리되기 쉬움
- 사용자 입장에서 "정말 취소된 것인지", "결제가 실패한 것인지", "서버 확인이 필요한 것인지" 구분이 어려움

이를 해결하기 위해 `PaymentStatus`를 별도로 도입했다.

```java
public enum PaymentStatus {
    READY,
    PROCESSING,
    PAID,
    FAILED,
    CANCELLED,
    UNKNOWN
}
```

적용 원칙은 다음과 같다.

- `status` : 주문/예매 자체의 상태
- `paymentStatus` : 결제 처리 상태

이렇게 분리한 뒤 상태 전이는 다음처럼 정리했다.

- 주문/예매 생성 직후: `status=대기`, `paymentStatus=READY`
- 결제 요청 직전: `paymentStatus=PROCESSING`
- 결제 성공 후 내부 확정 완료: `status=완료`, `paymentStatus=PAID`
- 카드 한도 초과 등 결제 실패 확정: `status=대기`, `paymentStatus=FAILED`
- 외부 서버 장애/타임아웃 등 결과 미확정: `status=대기`, `paymentStatus=UNKNOWN`
- 결제 취소가 실제로 완료된 경우: `status=취소`, `paymentStatus=CANCELLED`

즉, 더 이상 "결제 실패 = 주문 취소"로 단순화하지 않고, **비즈니스 상태와 결제 상태를 분리해서 표현**하도록 바꿨다.

### 3-8. 결제 실패 시 즉시 취소하지 않도록 변경

기존 보상 로직은 결제 실패나 서버 오류가 발생하면 대기 주문/예매를 바로 `취소`로 돌리는 방식이었다.

하지만 이 방식은 다음과 같은 한계가 있었다.

- 실제로는 결제가 단순 실패한 것뿐인데 주문 자체가 사라진 것처럼 보임
- 외부 결제 서버 장애처럼 결과를 아직 모르는 상황도 `취소`로 굳어질 수 있음

그래서 이번에는 실패 원인에 따라 다르게 처리하도록 바꿨다.

- 결제 실패 확정: `paymentStatus=FAILED`
- 외부 서버 오류/타임아웃: `paymentStatus=UNKNOWN`
- 재고 부족처럼 결제 후 보상 취소가 실제로 일어난 경우만 `status=취소`, `paymentStatus=CANCELLED`

이 변경으로 인해 사용자는 자신의 주문/예매가 "정말 취소된 것인지"와 "결제만 실패한 것인지"를 구분해서 볼 수 있게 되었다.

또한 조회 응답 DTO에도 `paymentStatus`를 포함시켜 클라이언트에서도 상태를 명확히 표현할 수 있도록 했다.

### 3-9. 만료 스케줄러가 결제 상태까지 함께 보도록 수정

`PendingOrderExpirationService`는 원래 `status=대기`이고 생성된 지 5분이 지난 주문/예매를 모두 자동 취소하고 있었다.

문제는 결제 상태를 분리한 뒤에도 스케줄러가 여전히 `status`만 보고 자동 취소하면, 다음과 같은 문제가 생길 수 있다는 점이었다.

- `paymentStatus=FAILED` : 자동 취소되어도 비교적 자연스러움
- `paymentStatus=UNKNOWN` : 실제 결제 결과를 아직 모르는 상태인데도 자동 취소될 수 있음
- 예매의 경우 `UNKNOWN` 상태인데 좌석까지 바로 풀려버릴 수 있음

이를 막기 위해 만료 조건을 `status`뿐 아니라 `paymentStatus`까지 함께 보도록 바꿨다.

현재 자동 만료 대상은 다음과 같다.

- `status=대기`
- `paymentStatus in (READY, FAILED)`
- 생성 후 5분 초과

반대로 다음 상태는 자동 만료 대상에서 제외했다.

- `PROCESSING`
- `UNKNOWN`

예매 좌석 삭제 쿼리도 동일한 조건을 사용하도록 맞춰서, 결제 미확정 상태의 좌석이 의도치 않게 해제되지 않도록 했다.

### 3-10. 이번 변경으로 정리된 현재 정책

- 외부 결제 호출은 DB 트랜잭션 밖에서 수행한다
- 주문/예매 상태와 결제 상태는 분리해서 관리한다
- 결제 실패는 곧바로 주문 취소로 보지 않고 `paymentStatus`로 표현한다
- 결제 결과가 불명확한 경우 `UNKNOWN`으로 남긴다
- 5분 만료 스케줄러는 `READY`, `FAILED` 상태만 자동 취소한다
- TODO : `UNKNOWN`, `PROCESSING`은 추후 별도 확인 흐름을 붙일 수 있도록 남겨둔다

### 3-11. 매점 주문 취소 API 추가

미션 요구사항에 맞춰 매점 주문도 취소 API를 추가했다.

적용한 정책은 예매 취소와 유사하지만, 매점 주문은 **재고 복원 여부**가 함께 고려되어야 했다.

- `대기` 주문: 외부 결제가 완료되지 않은 상태이므로 내부 주문만 바로 취소
- `완료` 주문: 외부 결제를 먼저 취소한 뒤 내부 주문을 취소하고 재고를 복원

즉, 매점 주문 취소는 단순히 상태값만 `취소`로 바꾸는 것이 아니라,
**결제 취소와 재고 복원까지 포함된 보상 흐름**으로 처리하도록 만들었다.

Controller에는 다음 API를 추가했다.

```java
@PostMapping("/{orderId}/cancel")
public ApiResponse<Void> cancelFoodOrder(
        @AuthenticationPrincipal UserDetailsImpl userDetails,
        @PathVariable Long orderId) {

    Long userId = userDetails.getUser().getId();
    foodPaymentFacade.cancelOrder(userId, orderId);
    return ApiResponse.onSuccess("매점 주문 취소 성공");
}
```

### 3-12. 완료 주문 취소 시 재고 복원 처리

완료된 매점 주문을 취소할 때는 외부 결제 취소 성공 이후,
기존에 차감했던 재고를 다시 복원해야 데이터 정합성이 맞는다.

이를 위해 `FoodOrderService.cancelOrderAfterPaymentCancellation()`에서

- 주문의 음식 항목을 `foodId` 기준으로 정렬한 뒤
- 각 재고 row를 락으로 다시 조회하고
- 차감했던 수량만큼 `increaseStock()`으로 복원
- 마지막으로 주문 상태를 `취소`, 결제 상태를 `CANCELLED`로 변경

하도록 구현했다.

이 흐름은 "완료 주문 취소 시 재고가 실제로 복원되는지"를 검증하는 통합 테스트로도 확인했다.

검증한 시나리오는 다음과 같다.

- 초기 재고 10개
- 주문 결제 완료 후 재고 8개
- 주문 취소 후 재고 10개로 복원
- 주문 상태 `완료 -> 취소`
- 결제 상태 `PAID -> CANCELLED`

### 3-13. 조회 구조 리팩토링

#### 조회 정책 통합

리팩토링을 진행하면서 여러 서비스에 흩어져 있던 `findById(...).orElseThrow(...)` 패턴도 정리했다.

초기에는 `MovieService`, `ReviewService`, `TheaterService`, `ReservationService`, `FoodOrderService` 등이
각자 `getUser`, `getMovie` 같은 helper 메서드를 따로 들고 있었다.
이 방식은 빠르게 작성하기는 쉽지만,

- 동일한 예외 정책이 여러 군데에 중복되고
- 조회 책임이 분산되어 메서드 위치를 기억하기 어려우며
- 조회 정책이 바뀔 때 수정 지점이 많아지는 문제가 있었다.

그래서 공통 조회 책임을 다음처럼 모았다.

- `UserService.getUser(userId)`
- `MovieService.getMovie(movieId)`

이후 다른 서비스들은 직접 Repository를 호출하지 않고,
해당 서비스 메서드를 주입받아 재사용하도록 통일했다.

이렇게 정리한 뒤에는 "유저 조회 정책은 `UserService`", "영화 조회 정책은 `MovieService`"처럼
책임 위치가 더 명확해졌다.

#### Mapper와 QueryService 분리

조회 로직이 늘어나면서 서비스 계층 안에

- Repository 조회
- DTO 조립
- 응답 포맷 매핑

이 한 메서드 안에 섞여 읽는 흐름이 길어지는 문제가 있었다.

이를 개선하기 위해 조회 전용 흐름을 다음처럼 분리했다.

- `Controller -> QueryService -> Mapper`

적용한 컴포넌트는 다음과 같다.

- `MovieQueryService`, `TheaterQueryService`, `ReviewQueryService`
- `ScheduleQueryService`, `FoodOrderQueryService`, `ReservationQueryService`
- `MovieMapper`, `TheaterMapper`, `ReviewMapper`
- `ScheduleMapper`, `FoodOrderMapper`, `ReservationMapper`

이 구조로 바꾼 뒤에는

- Command Service는 상태 변경에 집중하고
- Query Service는 조회와 화면 응답 조립에 집중하며
- Mapper는 DTO 변환만 담당하게 되었다.

결과적으로 서비스 메서드 길이가 줄고,
읽기/쓰기 책임이 이전보다 훨씬 명확해졌다.

### 3-14. 객체지향 설계 리팩토링

#### 도메인 객체 생성 책임을 엔티티로 이동

초기 관리자 서비스에서는 `Theater.builder()`, `Food.builder()`, `TheaterFood.builder()`를 직접 호출해
도메인 객체를 생성하고 있었다.

이 구조에서는

- 기본 상태값(`isAvailable=true`, `amount=0`)이 서비스에 노출되고
- 생성 규칙이 서비스마다 흩어질 수 있으며
- 엔티티가 자신의 생성 불변식을 스스로 설명하지 못하는 문제가 있었다.

그래서 생성 책임을 엔티티 내부의 정적 팩토리 메서드로 이동했다.

```java
public static Theater create(...) { ... }
public static Food create(...) { ... }
public static TheaterFood create(Theater theater, Food food) { ... }
```

이후 서비스는 더 이상 builder의 세부 필드를 직접 알지 않고,
"무엇을 만든다"는 의도만 표현하도록 단순화했다.

즉, 도메인 객체의 **초기 상태와 생성 규칙은 도메인 안에서 관리**하도록 정리한 것이다.

### 3-15. 입력 검증 및 예외 처리 보강

#### 리뷰 별점 검증 보강

리뷰 별점은 단순 `null` 여부만 확인하면
음수나 범위 초과 값이 그대로 저장될 수 있고,
이 값이 `MovieStatistics.averageRating` 계산에 반영되면 통계 데이터가 오염될 수 있다.

그래서 리뷰 생성 시 별점이

- `0.5 ~ 5.0` 범위 안에 있고
- `0.5` 단위인지

를 서비스 레벨에서 함께 검증하도록 보강했다.

이 검증을 통해 비정상 입력이 도메인 통계값으로 흘러들어가는 것을 막을 수 있게 되었다.

#### 중복 찜 처리 시 무결성 예외 숨김 방지

영화 찜 / 영화관 찜은 중복 요청이 들어왔을 때
동시성 상황에서는 `DataIntegrityViolationException`이 발생할 수 있다.

초기 구현은 이 예외를 만나면 무조건 성공처럼 반환했는데,
이 방식은 "진짜 중복"뿐 아니라

- 다른 제약조건 위반
- 잘못된 스키마 상태
- 예상하지 못한 DB 무결성 오류

까지 모두 조용히 숨겨버릴 위험이 있었다.

이를 개선해,

- 이미 해당 찜 레코드가 실제로 존재하는 경우에만 예외를 무시하고
- 그 외 무결성 예외는 그대로 다시 던지도록

처리 기준을 명확히 했다.

즉, "동시성으로 인한 중복 찜"만 허용하고,
실제 장애는 성공처럼 숨기지 않도록 변경했다.

### 3-16. 성능 최적화

#### 좌석 예매 N+1 문제 개선

초기 `ReservationService.processSeatReservations()`는 좌석 수만큼 반복문을 돌면서

- `seatRepository.findById(seatId)`
- `reservationSeatRepository.existsByMovieScreenIdAndSeatIdAndReservation_StatusIn(...)`

를 매번 호출하고 있었다.

이 구조는 요청 좌석 수만큼 쿼리가 늘어나는 전형적인 N+1 패턴이었고,
예매처럼 락과 트랜잭션이 함께 걸리는 흐름에서는 DB 커넥션 점유 시간을 더 길게 만들 수 있었다.

이를 개선하기 위해

- 좌석 엔티티는 `findAllByIdInWithScreen(seatIds)`로 한 번에 조회하고
- 이미 점유된 좌석은 `findReservedSeatIds(...)`로 한 번에 조회한 뒤
- 서비스 메모리에서 최종 검증

하는 방식으로 바꿨다.

이후 예매 생성 시 좌석 수가 늘어나더라도
좌석 조회와 중복 검증 쿼리 수가 선형적으로 증가하지 않도록 정리되었다.

</details>

<details><summary><h1>배포</h1></summary>

- 도커 실행
<img width="571" height="326" alt="image" src="https://github.com/user-attachments/assets/456e4a9f-ed4b-4701-bcff-7575ea965f6d" />

- 도커 컨테이너 확인
  `shinae@shinaeui-MacBookAir downloads % docker ps
CONTAINER ID   IMAGE          COMMAND                   CREATED         STATUS         PORTS                                         NAMES
a4936b9a9daf   mysql:8.0.46   "docker-entrypoint.s…"   2 minutes ago   Up 2 minutes   0.0.0.0:3306->3306/tcp, [::]:3306->3306/tcp   some-mysql`

- 로컬에서 도커 확인
  <img width="1257" height="694" alt="image" src="https://github.com/user-attachments/assets/783cbb0b-1780-470e-944e-1488ca5e0b5e" />

- ec2로 전송 및 받기
```bash
docker push shinae1023/ceos-app:latest
The push refers to repository [docker.io/shinae1023/ceos-app]
0d3dcfe2168e: Pushed 
8127c8426a01: Pushed 
5f83e74af606: Pushed 
afbbab386f63: Pushed 
5df7fb31528c: Pushed 
3687f9de5568: Pushed 
486a631f69c8: Pushed 
818154cda96d: Pushed 
latest: digest: sha256:314931c53a425ffa6625d90d68560f0b187cbcb4c4ab31fbd42276bcdea13fef size: 856
shinae@shinaeui-MacBookAir shinae1023 % docker buildx create --use --name multiarch-builder
docker buildx inspect --bootstrap
docker buildx build \
  --platform linux/amd64 \
  -t shinae1023/ceos-app:latest \
  --push \
  .
```

```
docker pull shinae1023/ceos-app:latest
latest: Pulling from shinae1023/ceos-app
b40150c1c271: Pull complete 
8a89ec8f5419: Pull complete 
ea035da72e5f: Pull complete 
1b87cf85ada1: Pull complete 
3c5d8083e928: Pull complete 
166e3ac40fca: Pull complete 
53feb4f7e8f6: Pull complete 
76af49cb70c6: Download complete 
Digest: sha256:38d971b756a9790aef213fd63ba43330d5155168d855a4cb6f1ebc22d0156df0
Status: Downloaded newer image for shinae1023/ceos-app:latest
docker.io/shinae1023/ceos-app:latest
```



- ec2에서 도커 실행
  `docker run -d   --name ceos-app   -p 8080:8080   --env-file ~/.env   shinae1023/ceos-app:latest
9c84da5dffb76145163c47e373d7c23ffd303c5b3209f28a0d341c7d5bf4ee18`

- ec2에서 도커 확인
  `docker ps
CONTAINER ID   IMAGE                        COMMAND               CREATED         STATUS         PORTS                                         NAMES
9c84da5dffb7   shinae1023/ceos-app:latest   "java -jar app.jar"   4 seconds ago   Up 4 seconds   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp   ceos-app`

- 배포 성공
<img width="1419" height="706" alt="image" src="https://github.com/user-attachments/assets/97a813bc-e5bd-4c9f-9380-3a3fd75a47c1" />

---

## Docker / EC2 배포 가이드

### 1. 로컬에서 jar 빌드

```bash
./gradlew build
```

- 실행 jar: `build/libs/ceos-0.0.1-SNAPSHOT.jar`
- `plain.jar`가 아니라 Spring Boot 실행 jar를 사용해야 한다.

### 2. Docker 이미지 빌드

프로젝트 루트의 `Dockerfile` 기준으로 빌드한다.

```bash
docker build -t ceos-app .
```

멀티 아키텍처 환경(Mac arm64에서 빌드 후 amd64 EC2에 배포)까지 고려하면 아래 방식이 안전하다.

```bash
docker buildx create --use --name multiarch-builder
docker buildx inspect --bootstrap
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t shinae1023/ceos-app:latest \
  --push \
  .
```

### 3. Docker Compose로 로컬 실행

`docker-compose.yml`은 앱 컨테이너만 띄우고, DB는 외부 MySQL/RDS를 사용하도록 구성되어 있다.

```bash
docker compose up -d --build
docker compose logs -f app
docker compose down
```

### 4. 환경변수 파일 준비

로컬 Docker나 EC2 실행 시 `.env` 파일을 사용한다.

예시:

```env
SPRING_PROFILES_ACTIVE=prod

RDS_ENDPOINT=your-rds-endpoint.ap-northeast-2.rds.amazonaws.com
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

PAYMENT_SERVER_URL=https://ceos.diggindie.com
PAYMENT_STORE_ID=shinae1023
PAYMENT_API_SECRET=your-payment-api-secret

JWT_SECRET_KEY=your-base64-jwt-secret
```

주의:

- `SPRING_PROFILES_ACTIVE=prod`가 반드시 있어야 한다.
- `application-prod.yml`은 `RDS_ENDPOINT`, `DB_USERNAME`, `DB_PASSWORD`를 사용한다.
- `JWT_SECRET_KEY`는 Base64 문자열이어야 한다.

### 5. Docker Hub에 이미지 올리기

```bash
docker login
docker tag ceos-app shinae1023/ceos-app:latest
docker push shinae1023/ceos-app:latest
```

Mac에서 만든 이미지를 EC2에서 받으려면 `linux/amd64` 또는 멀티아키 이미지로 push 해야 한다.

### 6. EC2에서 이미지 pull 및 실행

#### 6-1. Docker 권한 설정

```bash
sudo usermod -aG docker ubuntu
newgrp docker
```

권한 반영 전에는 `sudo docker ...`로 실행할 수 있다.

#### 6-2. 이미지 pull

```bash
docker pull shinae1023/ceos-app:latest
```

#### 6-3. 환경변수 파일 업로드 또는 생성

로컬에서 복사:

```bash
scp -i <key.pem> .env ubuntu@<EC2_PUBLIC_IP>:~/.env
```

또는 EC2에서 직접 생성:

```bash
nano ~/.env
```

#### 6-4. 컨테이너 실행

```bash
docker rm -f ceos-app
docker run -d \
  --name ceos-app \
  -p 8080:8080 \
  --env-file ~/.env \
  shinae1023/ceos-app:latest
```

#### 6-5. 실행 확인

```bash
docker ps
docker logs -f ceos-app
```

정상 기동 기준:

- `docker ps`에서 `ceos-app`이 `Up` 상태
- 로그에 `Tomcat started on port 8080`
- 로그에 `Started Application`

### 7. AWS RDS 연결 시 체크 포인트

- `RDS_ENDPOINT`가 실제 endpoint인지 확인
- `DB_USERNAME`, `DB_PASSWORD`가 정확한지 확인
- RDS Security Group에서 현재 서버의 3306 접근이 허용되어야 함
- 로컬 Docker 테스트 시에는 내 공인 IP 허용이 필요할 수 있음

### 8. Nginx + 80/443 + 도메인 연결 (여기서부턴 도메인 연결 후 사용)

배포 구조:

- Spring Boot 컨테이너: `8080`
- Nginx: `80`, `443`
- Nginx가 `127.0.0.1:8080`으로 reverse proxy

#### 8-1. 보안그룹 설정

- `80/tcp` 허용
- `443/tcp` 허용
- `8080/tcp`는 점검용으로만 잠시 열고, 운영 시 닫는 것을 권장

#### 8-2. Nginx 설치

```bash
sudo apt update
sudo apt install -y nginx
sudo systemctl status nginx
```

#### 8-3. Nginx 설정

```bash
sudo nano /etc/nginx/sites-available/ceos-app
```

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

활성화:

```bash
sudo ln -s /etc/nginx/sites-available/ceos-app /etc/nginx/sites-enabled/ceos-app
sudo nginx -t
sudo systemctl reload nginx
```

#### 8-4. HTTPS 적용

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
sudo certbot renew --dry-run
```

### 9. 최종 접속 확인

- EC2 직접 확인: `http://<EC2_PUBLIC_IP>:8080`
- Swagger: `http://<EC2_PUBLIC_IP>:8080/swagger-ui/index.html`
- 도메인/Nginx 적용 후: `https://your-domain.com/swagger-ui/index.html`

### 10. CI와 CD에서 Docker가 쓰이는 위치

#### CI

CI의 목적은 PR 또는 `main` 브랜치 변경 시 코드가 깨지지 않았는지 검증하는 것이다.

- GitHub Actions에서 `./gradlew test` 실행
- 유닛 테스트가 통과하면 `./gradlew build -x test` 실행
- 테스트와 빌드가 모두 성공해야만 이후 단계 진행
- 이 단계에서는 배포하지 않음

추가로 GitHub Branch Protection에서 `CI` 워크플로를 required status check로 설정하면, `CI` 성공 전에는 `main` 브랜치로 merge할 수 없게 만들 수 있다.

현재 워크플로 파일:

- `.github/workflows/ci.yml`

#### CD

CD의 목적은 `main` 브랜치에 머지된 검증 완료 코드를 실제 서버에 배포하는 것이다.

- `CI`가 성공한 `main` push만 감지
- jar를 다시 빌드
- Docker 이미지 생성
- Docker Hub로 push
- EC2에 SSH 접속
- 새 이미지를 후보 컨테이너로 먼저 실행
- `/health-check` 응답이 정상인지 확인
- 검증 성공 시에만 실제 서비스 컨테이너 교체
- 교체 실패 시 직전 정상 이미지로 롤백

현재 워크플로 파일:

- `.github/workflows/cd.yml`

### 11. 왜 jar 배포보다 Docker 배포가 편한가

#### jar 배포

- 서버에 Java 버전이 직접 설치되어 있어야 함
- 서버마다 환경 차이로 실행 문제가 생길 수 있음
- 배포 시 jar 업로드, 프로세스 종료, 재실행을 직접 관리해야 함

#### Docker 배포

- 앱 실행 환경(Java, 실행 명령, 설정 구조)을 이미지에 함께 묶을 수 있음
- 로컬, EC2, 다른 서버에서 동일한 방식으로 실행 가능
- 서버에는 Docker만 있으면 됨
- 배포 시 새 이미지만 pull 받아 교체하면 되므로 운영 흐름이 단순해짐

즉, 이 프로젝트에서는 `jar`를 직접 배포하는 것이 아니라, `jar를 포함한 Docker image`를 배포 단위로 사용한다.

### 12. 현재 워크플로 기준 실제 배포 흐름도

#### 1) Pull Request 생성

- 개발 브랜치에서 `main`으로 PR 생성
- `CI` 실행
- `./gradlew test` 통과 여부 확인
- `./gradlew build -x test` 통과 여부 확인
- 실패 시 머지 전 수정
- Branch Protection에 의해 `CI` 성공 전에는 merge 불가

#### 2) PR 머지

- `main` 브랜치에 push 발생
- `CI` 다시 실행
- `CI` 성공 시에만 `CD` 실행

#### 3) CD 배포

- GitHub Actions가 Docker 이미지를 빌드
- `linux/amd64`, `linux/arm64` 멀티 아키텍처 이미지 생성
- Docker Hub에 `latest`, `commit SHA` 태그로 push
- GitHub Actions가 EC2에 SSH 접속
- EC2에서 새 `commit SHA` 이미지 pull
- `ceos-app-candidate`를 `8081` 포트로 먼저 실행
- `curl http://127.0.0.1:8081/health-check`로 배포 후보 검증
- 검증 성공 후에만 기존 `ceos-app`을 내리고 새 컨테이너를 `8080` 포트에 실행
- 새 컨테이너가 최종 기동에 실패하면 직전 정상 이미지로 다시 실행
- 따라서 CD 실패 시 서버가 빈 상태로 멈추지 않고, 이전 성공 배포본을 유지하도록 구성

### 13. GitHub Actions Secrets

현재 CD 워크플로에서 사용하는 GitHub Secrets는 아래와 같다.

- `DOCKERHUB_USERNAME`: Docker Hub 계정명
- `DOCKERHUB_TOKEN`: Docker Hub Personal Access Token
- `EC2_HOST`: EC2 퍼블릭 IP 또는 도메인
- `EC2_USERNAME`: 예) `ubuntu`
- `EC2_SSH_KEY`: EC2 접속용 private key 전체 내용

민감한 애플리케이션 설정값은 GitHub Secrets가 아니라 EC2의 `~/.env`에 둔다.

<img width="1045" height="214" alt="image" src="https://github.com/user-attachments/assets/3ad6181b-fad6-4087-9a35-2e766bf32a83" />

배포 환경을 구축할 때 핵심이 되는 네트워크 구조와 보안 구성 요소들에 대한 설명임.

---

## 1. Subnet (서브넷)

VPC(Virtual Private Cloud)라는 커다란 네트워크를 다시 여러 개의 작은 네트워크로 쪼갠 단위를 의미함. 네트워크를 효율적으로 관리하고 보안 설계를 하기 위해 사용


### **Public Subnet vs Private Subnet**

네트워크의 **인터넷 연결 가능 여부**에 따라 구분됨.

* **Public Subnet (퍼블릭 서브넷):** 외부 인터넷과 직접 통신할 수 있는 영역임. 인터넷 게이트웨이(Internet Gateway)와 연결된 라우팅 테이블을 가지고 있으며, 주로 웹 서버나 로드 밸런서처럼 사용자가 직접 접속해야 하는 자원을 배치함.
* **Private Subnet (프라이빗 서브넷):** 외부 인터넷에서 직접 접근할 수 없는 폐쇄적인 영역임. 보안이 중요한 데이터베이스(DB)나 내부 로직을 처리하는 애플리케이션 서버를 배치함. 외부 인터넷으로 나가기 위해서는 **NAT Gateway**라는 별도의 장치가 필요함.

* Private Subnet은 외부에서 들어오는 것은 막혀 있지만, 내부 서버가 업데이트 등을 위해 외부 인터넷에 있는 패키지를 받아와야 할 때가 있음. 이때 내부에서 외부로 나가는 통로 역할만 해주는 장치가 NAT Gateway

## 2. Bastion Host (배스천 호스트)

외부 인터넷에서 차단된 **Private Subnet 내의 인스턴스에 관리자가 접속하기 위해 사용하는 일종의 '통로' 역할을 하는 서버**임.

* **역할:** 보안상의 이유로 DB 서버 등은 Private Subnet에 숨겨두는데, 관리자가 설정을 변경하거나 점검을 해야 할 때 Public Subnet에 있는 Bastion Host에 먼저 접속한 뒤, 거기서 다시 내부망을 통해 Private Subnet으로 들어감.
* **보안성:** Bastion Host에 대해서는 엄격한 접근 제어(특정 IP만 접속 허용 등)를 적용하여 보안 침해 사고를 방지


## 3. Subnet CIDR (서브넷 CIDR)

서브넷을 만들 때 해당 네트워크에서 **사용할 IP 주소의 범위를 지정하는 방식**임. `10.0.1.0/24`와 같은 형태로 표기함.

* **구조:** `[IP 주소]/[Prefix Length]` 형태임.
* **계산 방식:** `/24`는 앞의 24비트가 네트워크 주소임을 의미하며, 나머지 8비트($32 - 24 = 8$)를 통해 $2^8 = 256$개의 IP 주소를 사용할 수 있다는 뜻
* **주의사항:** 클라우드 환경(AWS 등)에서는 서브넷의 첫 번째 주소부터 마지막 주소 중 일부(보통 5개)를 네트워크 관리용으로 예약하여 사용자가 직접 쓸 수 없으므로 이를 계산에 넣어야 함


</details>

<details><summary><h1>부하테스트 및 모니터링</h1></summary>

### 아키텍처 구조도
<img width="2624" height="1816" alt="image" src="https://github.com/user-attachments/assets/d8867f58-83c3-40b3-9c1b-9e4fd492b6b2" />

- 병목 가능성 높은 지점
  External Payment API
  DB/RDS
  Spring thread pool / connection pool
  JPA transaction/locking
  
아키텍처상 병목 가능성이 가장 높은 지점은 외부 Payment API 호출 구간과 MySQL(RDS) 접근 구간이며, 이 둘의 지연이 누적되면 Spring Boot의 요청 처리 스레드가 점유되어 전체 처리율 저하와 timeout 증가로 이어질 수 있다.

### 모니터링
alloy, loki, grafana, prometheus 도커 연결
<img width="1341" height="156" alt="image" src="https://github.com/user-attachments/assets/ef1c3416-695b-46bd-802d-59ea22f282fa" />

### loki 데이터 추가
<img width="1196" height="349" alt="image" src="https://github.com/user-attachments/assets/2e7717b5-d801-44c0-8ace-3405f334864f" />
<img width="1158" height="308" alt="image" src="https://github.com/user-attachments/assets/d7274820-7a23-43d8-bfb9-54c89356498e" />

### 로그 확인
<img width="1417" height="662" alt="image" src="https://github.com/user-attachments/assets/8ab2d6fd-757d-47c6-abec-4b8c7c786e20" />
<img width="1359" height="487" alt="image" src="https://github.com/user-attachments/assets/8534dfb8-bf89-4eb4-85bd-411883c3301d" />
<img width="1434" height="645" alt="image" src="https://github.com/user-attachments/assets/1c75a599-a30b-43ea-a418-83d7e064c1dc" />

### k6 부하테스트

- 외부 페이먼트 서버에 부하테스트 (결제 요청)
```
  █ TOTAL RESULTS 

    checks_total.......: 3589   13.286044/s
    checks_succeeded...: 79.13% 2840 out of 3589
    checks_failed......: 20.86% 749 out of 3589

    ✓ auth status is 200
    ✗ payment status is 200
      ↳  66% — ✓ 1433 / ✗ 724
    ✗ lookup status is 200
      ↳  98% — ✓ 1406 / ✗ 25

    HTTP
    http_req_duration..............: avg=6.9s     min=11.17ms med=24.82ms max=59.99s p(90)=30.03s   p(95)=58.05s  
      { expected_response:true }...: avg=655.29ms min=11.17ms med=22.03ms max=59.87s p(90)=101.05ms p(95)=171.27ms
    http_req_failed................: 20.86% 749 out of 3589
    http_reqs......................: 3589   13.286044/s

    EXECUTION
    iteration_duration.............: avg=12.98s   min=1.01s   med=2.06s   max=2m0s   p(90)=36.95s   p(95)=1m0s    
    iterations.....................: 2139   7.918319/s
    vus............................: 22     min=1           max=199
    vus_max........................: 200    min=200         max=200

    NETWORK
    data_received..................: 2.7 MB 10 kB/s
    data_sent......................: 1.0 MB 3.8 kB/s
```
<img width="2848" height="2032" alt="image" src="https://github.com/user-attachments/assets/9e15aff8-29d2-4395-80b0-e38553a987c3" />
VU를 200까지 증가시키는 동안 초반 처리율은 약 39 req/s까지 상승했지만, 이후 결제 요청(payment_instant)에서 실패가 급증하며 처리율이 오히려 감소했다. 
실패율은 후반부에 60% 이상까지 증가했고, 성공 응답 기준 p95도 지속적으로 상승

- 병목 찾기
```
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'https://ceos.diggindie.com';
const USER_EMAIL = __ENV.USER_EMAIL || 'string';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'string';
const THEATER_ID = Number(__ENV.THEATER_ID || 1);
const FOOD_ID = Number(__ENV.FOOD_ID || 1);
const QUANTITY = Number(__ENV.QUANTITY || 1);
const MODE = __ENV.MODE || 'full_payment';

function buildOptions() {
  if (MODE === 'db_only') {
    return {
      stages: [
        { duration: '2m', target: 10 },
        { duration: '2m', target: 30 },
      ],
      thresholds: {
        'http_req_failed{name:create_food_order}': ['rate<0.05'],
        'http_req_duration{name:create_food_order}': ['p(95)<1000'],
        'http_req_duration{name:get_food_orders}': ['p(95)<700'],
      },
    };
  }

  return {
    stages: [
      { duration: '2m', target: 100 },
      { duration: '2m', target: 200 },
    ],
    thresholds: {
      'http_req_failed{name:create_food_order}': ['rate<0.05'],
      'http_req_failed{name:process_food_payment}': ['rate<0.10'],
      'http_req_duration{name:process_food_payment}': ['p(95)<3000'],
    },
  };
}

export const options = buildOptions();

export function setup() {
  const loginPayload = JSON.stringify({
    email: USER_EMAIL,
    password: USER_PASSWORD,
  });

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });

  check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login has access token': (r) => !!r.json('result.accessToken'),
  });

  const accessToken = loginRes.json('result.accessToken');

  if (!accessToken) {
    throw new Error(`로그인 실패: status=${loginRes.status}, body=${loginRes.body}`);
  }

  return { accessToken };
}

function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

function createFoodOrder(token) {
  const payload = JSON.stringify({
    theaterId: THEATER_ID,
    items: [
      {
        foodId: FOOD_ID,
        quantity: QUANTITY,
      },
    ],
  });

  const res = http.post(`${BASE_URL}/api/foods/orders/`, payload, {
    headers: authHeaders(token),
    tags: { name: 'create_food_order' },
  });

  check(res, {
    'create food order status is 200': (r) => r.status === 200,
    'create food order has order id': (r) => !!r.json('result'),
  });

  return res;
}

function getFoodOrders(token) {
  return http.get(`${BASE_URL}/api/foods/orders/`, {
    headers: authHeaders(token),
    tags: { name: 'get_food_orders' },
  });
}

function processFoodPayment(token, orderId) {
  return http.post(`${BASE_URL}/api/foods/orders/${orderId}/payments`, null, {
    headers: authHeaders(token),
    tags: { name: 'process_food_payment' },
  });
}

export default function (data) {
  const token = data.accessToken;

  if (MODE === 'db_only') {
    const createRes = createFoodOrder(token);
    const listRes = getFoodOrders(token);

    check(listRes, {
      'get food orders status is 200': (r) => r.status === 200,
    });

    if (createRes.status !== 200) {
      return;
    }

    sleep(1);
    return;
  }

  const createRes = createFoodOrder(token);
  if (createRes.status !== 200) {
    sleep(1);
    return;
  }

  const orderId = createRes.json('result');
  const paymentRes = processFoodPayment(token, orderId);

  check(paymentRes, {
    'process food payment status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
```

```
shinae@shinaeui-MacBookAir shinae1023 % BASE_URL=http://13.125.8.199:8080 \
USER_EMAIL=string \
USER_PASSWORD=string \
THEATER_ID=1 \
FOOD_ID=1 \
MODE=db_only \
K6_PROMETHEUS_RW_SERVER_URL=http://13.125.8.199:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS=p(95),p(99),min,max \
k6 run -o experimental-prometheus-rw k6-app-bottleneck.js


         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: k6-app-bottleneck.js
        output: Prometheus remote write (http://13.125.8.199:9090/api/v1/write)

     scenarios: (100.00%) 1 scenario, 30 max VUs, 4m30s max duration (incl. graceful stop):
              * default: Up to 30 looping VUs for 4m0s over 2 stages (gracefulRampDown: 30s, gracefulStop: 30s)



  █ THRESHOLDS 

    http_req_duration{name:create_food_order}
    ✗ 'p(95)<1000' p(95)=5.62s

    http_req_duration{name:get_food_orders}
    ✗ 'p(95)<700' p(95)=8.9s

    http_req_failed{name:create_food_order}
    ✓ 'rate<0.05' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 2228    8.987865/s
    checks_succeeded...: 100.00% 2228 out of 2228
    checks_failed......: 0.00%   0 out of 2228

    ✓ login status is 200
    ✓ login has access token
    ✓ create food order status is 200
    ✓ create food order has order id
    ✓ get food orders status is 200

    HTTP
    http_req_duration..............: avg=1.5s  min=28.13ms med=136.58ms max=59.69s p(90)=2.24s p(95)=6.56s 
      { expected_response:true }...: avg=1.5s  min=28.13ms med=136.58ms max=59.69s p(90)=2.24s p(95)=6.56s 
      { name:create_food_order }...: avg=1.54s min=28.13ms med=61.12ms  max=59.69s p(90)=2.01s p(95)=5.62s 
      { name:get_food_orders }.....: avg=1.45s min=85.37ms med=197.31ms max=59.39s p(90)=2.62s p(95)=8.9s  
    http_req_failed................: 0.00%  0 out of 1485
      { name:create_food_order }...: 0.00%  0 out of 742
    http_reqs......................: 1485   5.990565/s

    EXECUTION
    iteration_duration.............: avg=3.99s min=1.11s   med=1.27s    max=1m6s   p(90)=7.89s p(95)=15.88s
    iterations.....................: 742    2.993266/s
    vus............................: 21     min=0         max=29
    vus_max........................: 30     min=30        max=30

    NETWORK
    data_received..................: 306 MB 1.2 MB/s
    data_sent......................: 488 kB 2.0 kB/s




running (4m07.9s), 00/30 VUs, 742 complete and 0 interrupted iterations
default ✓ [======================================] 00/30 VUs  4m0s
ERRO[0248] thresholds on metrics 'http_req_duration{name:create_food_order}, http_req_duration{name:get_food_orders}' have been crossed 
```
<img width="2848" height="2640" alt="image" src="https://github.com/user-attachments/assets/133b8141-80e0-4ace-8c8d-b728ec47933b" />

DB 중심 부하테스트 결과, 주문 생성 API는 p95 529ms로 기준을 충족했지만 주문 조회 API는 p95 1.27초로 목표를 초과했다. 실패율은 0%였으므로 현재 문제는 기능 장애가 아니라 조회 경로의 성능 저하이며, 병목은 Spring-JPA-MySQL 구간, 특히 주문 조회 로직에 있을 가능성이 높다.
외부 결제 이전에 get_food_orders가 느린 가장 큰 이유는, 생성된 주문이 누적되는 상황에서 전체 주문+아이템을 매번 한 번에 다 조회하도록 짜여 있기 때문
-> 페이지네이션 도입
### 변경 내용:
- FoodOrderController GET /api/foods/orders/에 page, size 쿼리 파라미터 추가

- FoodOrderQueryService.java 전체 조회 대신 ID 페이지 조회 -> 상세 fetch 2단계로 변경

- FoodOrderRepository.java findPageIdsByUserId()와 findAllByIdInWithDetails() 추가

<img width="2848" height="2640" alt="image" src="https://github.com/user-attachments/assets/d99b6057-493a-42c4-97f1-b83267b61db5" />

```

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: k6-app-bottleneck.js
        output: Prometheus remote write (http://13.125.8.199:9090/api/v1/write)

     scenarios: (100.00%) 1 scenario, 30 max VUs, 4m30s max duration (incl. graceful stop):
              * default: Up to 30 looping VUs for 4m0s over 2 stages (gracefulRampDown: 30s, gracefulStop: 30s)



  █ THRESHOLDS 

    http_req_duration{name:create_food_order}
    ✓ 'p(95)<1000' p(95)=92.98ms

    http_req_duration{name:get_food_orders}
    ✓ 'p(95)<700' p(95)=87ms

    http_req_failed{name:create_food_order}
    ✓ 'rate<0.05' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 7997    32.51493/s
    checks_succeeded...: 100.00% 7997 out of 7997
    checks_failed......: 0.00%   0 out of 7997

    ✓ login status is 200
    ✓ login has access token
    ✓ create food order status is 200
    ✓ create food order has order id
    ✓ get food orders status is 200

    HTTP
    http_req_duration..............: avg=55.39ms min=24.51ms med=39.35ms max=7.05s p(90)=63.36ms p(95)=90.22ms
      { expected_response:true }...: avg=55.39ms min=24.51ms med=39.35ms max=7.05s p(90)=63.36ms p(95)=90.22ms
      { name:create_food_order }...: avg=58.68ms min=24.51ms med=37.57ms max=7.04s p(90)=64.7ms  p(95)=92.98ms
      { name:get_food_orders }.....: avg=50.31ms min=26.44ms med=40.98ms max=7.05s p(90)=62.09ms p(95)=87ms   
    http_req_failed................: 0.00%  0 out of 5331
      { name:create_food_order }...: 0.00%  0 out of 2665
    http_reqs......................: 5331   21.675265/s

    EXECUTION
    iteration_duration.............: avg=1.11s   min=1.05s   med=1.08s   max=8.12s p(90)=1.12s   p(95)=1.18s  
    iterations.....................: 2665   10.835599/s
    vus............................: 27     min=0         max=29
    vus_max........................: 30     min=30        max=30

    NETWORK
    data_received..................: 13 MB  54 kB/s
    data_sent......................: 1.8 MB 7.3 kB/s




running (4m05.9s), 00/30 VUs, 2665 complete and 0 interrupted iterations
default ✓ [======================================] 00/30 VUs  4m0s
```

### 개선 결과 정리
- 페이지네이션 적용 후 `get_food_orders`의 p95는 `1.27s -> 87ms`로 크게 개선되었다.
- `create_food_order`의 p95도 `529ms -> 92.98ms`로 감소해 기준치 `1s` 이내를 안정적으로 만족했다.
- 전체 p95는 약 `90ms`, 실패율은 `0%`로 유지되어 조회 경로의 성능 저하가 해소되었음을 확인했다.
- `data_received`는 `1.0GB -> 13MB`로 줄어들어, 기존 병목의 핵심 원인이 전체 주문 내역을 한 번에 조회하던 구조였음을 확인했다.

### 결론
기존 `get_food_orders`는 주문이 누적될수록 전체 주문과 주문 아이템을 모두 조회해 응답 크기와 조회 비용이 함께 증가하는 구조였다.  
페이지네이션 도입 이후 조회량이 제한되면서 DB/JPA 처리 부담이 줄었고, 주문 생성/조회 API 모두 30 VU 환경에서 안정적인 응답 속도를 보였다.

### full_payment로 외부 서버 호출해서 확인
<img width="2848" height="2640" alt="image" src="https://github.com/user-attachments/assets/0dd323fc-3bd3-4fa7-8b35-058a29a31fe7" />
<img width="2848" height="2640" alt="image" src="https://github.com/user-attachments/assets/8f10bb56-d659-405a-94e2-1d8d8f9cfcdf" />

```
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: k6-app-bottleneck.js
        output: Prometheus remote write (http://13.125.8.199:9090/api/v1/write)

     scenarios: (100.00%) 1 scenario, 200 max VUs, 4m30s max duration (incl. graceful stop):
              * default: Up to 200 looping VUs for 4m0s over 2 stages (gracefulRampDown: 30s, gracefulStop: 30s)



  █ THRESHOLDS 

    http_req_duration{name:process_food_payment}
    ✓ 'p(95)<3000' p(95)=1.11s

    http_req_failed{name:create_food_order}
    ✓ 'rate<0.05' rate=0.00%

    http_req_failed{name:process_food_payment}
    ✗ 'rate<0.10' rate=90.81%


  █ TOTAL RESULTS 

    checks_total.......: 32651  134.433061/s
    checks_succeeded...: 69.73% 22768 out of 32651
    checks_failed......: 30.26% 9883 out of 32651

    ✓ login status is 200
    ✓ login has access token
    ✓ create food order status is 200
    ✓ create food order has order id
    ✗ process food payment status is 200
      ↳  9% — ✓ 1000 / ✗ 9883

    HTTP
    http_req_duration.................: avg=605.14ms min=24.83ms med=176.66ms max=24.26s p(90)=768.35ms p(95)=1.16s
      { expected_response:true }......: avg=659.28ms min=24.83ms med=118.84ms max=24.24s p(90)=710.27ms p(95)=1.16s
      { name:process_food_payment }...: avg=504.64ms min=53.64ms med=209.23ms max=24.26s p(90)=781.08ms p(95)=1.11s
    http_req_failed...................: 45.40% 9883 out of 21767
      { name:create_food_order }......: 0.00%  0 out of 10883
      { name:process_food_payment }...: 90.81% 9883 out of 10883
    http_reqs.........................: 21767  89.620668/s

    EXECUTION
    iteration_duration................: avg=2.22s    min=1.08s   med=1.38s    max=26.51s p(90)=2.56s    p(95)=4.87s
    iterations........................: 10883  44.808275/s
    vus...............................: 106    min=1             max=199
    vus_max...........................: 200    min=200           max=200

    NETWORK
    data_received.....................: 11 MB  47 kB/s
    data_sent.........................: 7.5 MB 31 kB/s




running (4m02.9s), 000/200 VUs, 10883 complete and 0 interrupted iterations
default ✓ [======================================] 000/200 VUs  4m0s
ERRO[0243] thresholds on metrics 'http_req_failed{name:process_food_payment}' have been crossed 
```

full_payment 부하테스트 결과, 주문 생성 API는 정상 동작했지만 결제 처리 API의 실패율은 90.81%까지 상승했다. 그러나 로그 분석 결과 외부 결제 API 호출은 대부분 20ms 내외로 빠르게 성공했으며, 실제 실패 원인은 결제 이후 재고 차감 단계에서 발생한 OUT_OF_STOCK 예외였다. 따라서 이번 테스트의 주된 문제는 외부 결제 성능이 아니라 재고 소진으로 인한 비즈니스 실패이다.
ㅠㅠ -> 재고 100만개로 하고 다시 돌려보기

<img width="2848" height="2640" alt="image" src="https://github.com/user-attachments/assets/54d0a1c5-6aaa-4a09-b1d7-b1b63498f45b" />
```
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: k6-app-bottleneck.js
        output: Prometheus remote write (http://13.125.8.199:9090/api/v1/write)

     scenarios: (100.00%) 1 scenario, 200 max VUs, 4m30s max duration (incl. graceful stop):
              * default: Up to 200 looping VUs for 4m0s over 2 stages (gracefulRampDown: 30s, gracefulStop: 30s)



  █ THRESHOLDS 

    http_req_duration{name:process_food_payment}
    ✓ 'p(95)<3000' p(95)=1.08s

    http_req_failed{name:create_food_order}
    ✓ 'rate<0.05' rate=0.20%

    http_req_failed{name:process_food_payment}
    ✗ 'rate<0.10' rate=10.40%


  █ TOTAL RESULTS 

    checks_total.......: 35135  143.706329/s
    checks_succeeded...: 96.39% 33870 out of 35135
    checks_failed......: 3.60%  1265 out of 35135

    ✓ login status is 200
    ✓ login has access token
    ✗ create food order status is 200
      ↳  99% — ✓ 11695 / ✗ 24
    ✗ create food order has order id
      ↳  99% — ✓ 11695 / ✗ 24
    ✗ process food payment status is 200
      ↳  89% — ✓ 10478 / ✗ 1217

    HTTP
    http_req_duration.................: avg=529.91ms min=23.73ms med=109.77ms max=44.89s p(90)=792.1ms  p(95)=1.03s
      { expected_response:true }......: avg=409.17ms min=23.73ms med=108.77ms max=40.52s p(90)=784.93ms p(95)=1.02s
      { name:process_food_payment }...: avg=516.46ms min=51.11ms med=135.65ms max=44.45s p(90)=824.44ms p(95)=1.08s
    http_req_failed...................: 5.30%  1241 out of 23415
      { name:create_food_order }......: 0.20%  24 out of 11719
      { name:process_food_payment }...: 10.40% 1217 out of 11695
    http_reqs.........................: 23415  95.770135/s

    EXECUTION
    iteration_duration................: avg=2.06s    min=1.07s   med=1.22s    max=46.05s p(90)=2.6s     p(95)=2.95s
    iterations........................: 11719  47.932104/s
    vus...............................: 63     min=0             max=199
    vus_max...........................: 200    min=200           max=200

    NETWORK
    data_received.....................: 12 MB  51 kB/s
    data_sent.........................: 8.1 MB 33 kB/s




running (4m04.5s), 000/200 VUs, 11719 complete and 0 interrupted iterations
default ✓ [======================================] 000/200 VUs  4m0s
```

process_food_payment p95 = 1.08s
process_food_payment 실패율 = 10.40%
create_food_order 실패율 = 0.20%
전체 요청 처리량 95.77 req/s
전체 반복 처리량 47.93 iter/s
로그에 보이는 실패는 대부분 Payment instant request failed ... status=500 code=PAYMENT_FAILED
주문 생성은 거의 다 성공 -> 결제 요청까지도 정상 진입 -> 외부 결제 instant API에서 일부 500
-> 그 결과 FoodPaymentFacade가 PAYMENT_FAILED로 종료
따라서 현재 병목은 내부 DB 조회보다는 외부 결제 연동 안정성에 더 가까움

</details>
