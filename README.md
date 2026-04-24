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
