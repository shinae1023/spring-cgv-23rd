# 🎬 CGV 클론코딩 서비스 소개

본 프로젝트는 실제 영화 예매 서비스인 CGV를 기반으로, 
영화 조회, 예매, 좌석 선택, 찜 기능, 매점 구매 등의 핵심 기능을 백엔드 중심으로 구현한 서비스입니다.

단순 CRUD 구현을 넘어,
다음과 같은 실제 서비스에서 발생하는 문제를 고려하여 설계하였습니다.

- 상영 회차별 좌석 관리
- 동일 좌석 중복 예매 방지
- 영화/극장 찜 기능
- 영화 통계 및 리뷰 관리
- 극장별 매점 재고 관리

### 📌 핵심 기능
- 영화관 조회 및 찜
- 영화 조회 (상세 정보, 리뷰, 통계 포함)
- 영화 예매 및 취소
- 좌석 선택 (상영관 기준)
- 영화 찜 기능
- 매점 주문 (극장별 재고 관리)

### ERD
https://www.erdcloud.com/d/br5ZwPJDjX9WvPYXH

<details>
<summary><h1>CGV 클론코딩 ERD 연관관계 및 제약조건 정리</h1></summary>

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

</details>
