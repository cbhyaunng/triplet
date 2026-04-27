# TRIPLET Server API

## 1. 목적

이 문서는 트립랫 안드로이드 앱이 서버와 데이터를 동기화하기 위한 REST API 계약을 정의한다.

MVP 기준으로 서버는 아래 역할을 맡는다.

- 사용자별 여행 데이터 백업
- 다중 기기 대비
- 여행 단위 통계 계산
- 지역별 소비 집중도 계산
- 향후 제휴/추천 기능 확장을 위한 구조화 데이터 저장

## 2. 기본 원칙

- API Base URL: `/v1`
- 포맷: `application/json`
- 인증: `Authorization: Bearer <access_token>`
- 시간: ISO-8601 UTC
- 통화: ISO 4217
- 금액: `amount_minor` 사용
  - 예: 12,300원 -> `12300`
- 모든 모바일 생성 레코드는 `client_id`를 포함한다.
- 배치 업로드 API는 idempotent 하게 동작한다.

## 3. 인증 전략

MVP에서는 외부 인증 공급자를 사용하고, 앱 서버는 JWT만 검증하는 방식을 추천한다.

### 권장 방식

- Supabase Auth
- Firebase Authentication
- 자체 JWT 발급 서버

### 서버에서 직접 다룰 최소 auth endpoint

- `GET /v1/me`
- `POST /v1/devices`

## 4. 공통 헤더

```http
Authorization: Bearer eyJ...
Content-Type: application/json
X-Client-Version: 1.0.0
X-Platform: android
X-Device-Id: a8b1c2d3
```

## 5. 공통 응답 형식

### 성공

```json
{
  "data": {},
  "meta": {
    "request_id": "req_01JXYZ",
    "server_time": "2026-04-25T12:00:00Z"
  }
}
```

### 실패

```json
{
  "error": {
    "code": "EXPENSE_VALIDATION_FAILED",
    "message": "amount_minor must be greater than 0",
    "details": {
      "field": "amount_minor"
    }
  },
  "meta": {
    "request_id": "req_01JXYZ",
    "server_time": "2026-04-25T12:00:00Z"
  }
}
```

## 6. 리소스 모델

## 6.1 User

```json
{
  "user_id": "usr_123",
  "email": "demo@example.com",
  "display_name": "Changmin",
  "created_at": "2026-04-25T12:00:00Z"
}
```

## 6.2 Trip

```json
{
  "trip_id": "trip_123",
  "client_id": "local-trip-uuid",
  "title": "오사카 3박 4일",
  "country_code": "JP",
  "city_name": "Osaka",
  "base_currency": "JPY",
  "display_currency": "KRW",
  "status": "ACTIVE",
  "start_date": "2026-05-03",
  "end_date": "2026-05-06",
  "started_at": "2026-05-03T01:00:00Z",
  "ended_at": null
}
```

## 6.3 Expense

```json
{
  "expense_id": "exp_123",
  "client_id": "local-expense-uuid",
  "trip_id": "trip_123",
  "source": "NOTIFICATION",
  "merchant_raw": "KB국민카드 스타벅스",
  "merchant_normalized": "STARBUCKS",
  "amount_minor": 12800,
  "currency_code": "KRW",
  "category": "CAFE",
  "occurred_at": "2026-05-03T03:15:21Z",
  "latitude": 34.70241,
  "longitude": 135.49592,
  "accuracy_m": 18.4,
  "place_id": "google-place-id",
  "place_name": "Starbucks Umeda",
  "area_code": "JP-27-OSAKA-KITA",
  "area_name": "Osaka Kita",
  "match_confidence": 0.92,
  "review_status": "AUTO_CONFIRMED",
  "manual_edited": false,
  "updated_at": "2026-05-03T03:15:30Z"
}
```

## 6.4 Location Sample

```json
{
  "location_sample_id": "loc_123",
  "client_id": "local-location-uuid",
  "trip_id": "trip_123",
  "latitude": 34.70241,
  "longitude": 135.49592,
  "accuracy_m": 18.4,
  "captured_at": "2026-05-03T03:14:58Z",
  "capture_reason": "PERIODIC"
}
```

## 7. Auth / Profile API

## 7.1 `GET /v1/me`

현재 로그인 사용자의 프로필과 요약 정보를 가져온다.

### Response

```json
{
  "data": {
    "user_id": "usr_123",
    "email": "demo@example.com",
    "display_name": "Changmin",
    "active_trip_id": "trip_123"
  }
}
```

## 7.2 `POST /v1/devices`

앱이 실행된 디바이스를 등록한다.

### Request

```json
{
  "device_id": "android-abc-001",
  "device_model": "Pixel 8",
  "app_version": "1.0.0",
  "os_version": "Android 15",
  "push_token": "fcm-token"
}
```

### Response

```json
{
  "data": {
    "device_id": "android-abc-001",
    "registered": true
  }
}
```

## 8. Trip API

## 8.1 `POST /v1/trips`

여행 생성

### Request

```json
{
  "client_id": "local-trip-uuid",
  "title": "오사카 3박 4일",
  "country_code": "JP",
  "city_name": "Osaka",
  "base_currency": "JPY",
  "display_currency": "KRW",
  "start_date": "2026-05-03",
  "end_date": "2026-05-06"
}
```

## 8.2 `GET /v1/trips`

여행 목록 조회

### Query

- `status=ACTIVE|FINISHED|ARCHIVED`
- `cursor=...`
- `limit=20`

## 8.3 `GET /v1/trips/{trip_id}`

여행 상세 조회

## 8.4 `PATCH /v1/trips/{trip_id}`

여행 제목, 날짜, 표시 통화 수정

## 8.5 `POST /v1/trips/{trip_id}/start`

여행 시작

### Request

```json
{
  "started_at": "2026-05-03T01:00:00Z"
}
```

## 8.6 `POST /v1/trips/{trip_id}/finish`

여행 종료

### Request

```json
{
  "ended_at": "2026-05-06T12:30:00Z"
}
```

## 9. Expense API

## 9.1 `POST /v1/trips/{trip_id}/expenses/batch`

지출 배치 업로드. 모바일 sync 핵심 endpoint.

### Request

```json
{
  "items": [
    {
      "client_id": "local-expense-uuid-1",
      "source": "NOTIFICATION",
      "merchant_raw": "KB국민카드 스타벅스",
      "merchant_normalized": "STARBUCKS",
      "amount_minor": 12800,
      "currency_code": "KRW",
      "category": "CAFE",
      "note": null,
      "occurred_at": "2026-05-03T03:15:21Z",
      "latitude": 34.70241,
      "longitude": 135.49592,
      "accuracy_m": 18.4,
      "place_id": "google-place-id",
      "place_name": "Starbucks Umeda",
      "area_code": "JP-27-OSAKA-KITA",
      "area_name": "Osaka Kita",
      "match_confidence": 0.92,
      "review_status": "AUTO_CONFIRMED",
      "manual_edited": false,
      "deleted": false,
      "updated_at": "2026-05-03T03:15:30Z"
    }
  ]
}
```

### Response

```json
{
  "data": {
    "accepted": [
      {
        "client_id": "local-expense-uuid-1",
        "expense_id": "exp_123",
        "status": "UPSERTED"
      }
    ],
    "rejected": []
  }
}
```

## 9.2 `GET /v1/trips/{trip_id}/expenses`

여행별 지출 목록 조회

### Query

- `cursor`
- `limit`
- `category`
- `review_status`
- `updated_since`

## 9.3 `PATCH /v1/trips/{trip_id}/expenses/{expense_id}`

지출 수정

### Request

```json
{
  "category": "FOOD",
  "note": "점심",
  "latitude": 34.70100,
  "longitude": 135.49700,
  "manual_edited": true,
  "updated_at": "2026-05-03T03:40:00Z"
}
```

## 9.4 `DELETE /v1/trips/{trip_id}/expenses/{expense_id}`

soft delete 처리

### Response

```json
{
  "data": {
    "deleted": true
  }
}
```

## 10. Location API

## 10.1 `POST /v1/trips/{trip_id}/locations/batch`

위치 샘플 배치 업로드

### Request

```json
{
  "items": [
    {
      "client_id": "local-location-uuid-1",
      "latitude": 34.70241,
      "longitude": 135.49592,
      "accuracy_m": 18.4,
      "captured_at": "2026-05-03T03:14:58Z",
      "capture_reason": "PERIODIC",
      "deleted": false
    }
  ]
}
```

### Response

```json
{
  "data": {
    "accepted": [
      {
        "client_id": "local-location-uuid-1",
        "location_sample_id": "loc_123",
        "status": "UPSERTED"
      }
    ],
    "rejected": []
  }
}
```

## 10.2 `GET /v1/trips/{trip_id}/locations`

지도 경로 복원을 위한 위치 샘플 조회

### Query

- `from`
- `to`
- `limit`

## 11. Map / Stats API

## 11.1 `GET /v1/trips/{trip_id}/stats/overview`

### Response

```json
{
  "data": {
    "total_amount_minor": 418000,
    "expense_count": 24,
    "days_count": 4,
    "average_per_expense_minor": 17416,
    "top_category": "SHOPPING",
    "top_area_name": "Osaka Kita"
  }
}
```

## 11.2 `GET /v1/trips/{trip_id}/stats/categories`

### Response

```json
{
  "data": [
    {
      "category": "FOOD",
      "amount_minor": 98000,
      "ratio": 0.2344
    },
    {
      "category": "SHOPPING",
      "amount_minor": 176000,
      "ratio": 0.4211
    }
  ]
}
```

## 11.3 `GET /v1/trips/{trip_id}/stats/daily`

### Response

```json
{
  "data": [
    {
      "date": "2026-05-03",
      "amount_minor": 132000
    },
    {
      "date": "2026-05-04",
      "amount_minor": 96000
    }
  ]
}
```

## 11.4 `GET /v1/trips/{trip_id}/stats/regions`

지역별 소비 집중도용 데이터

### Response

```json
{
  "data": [
    {
      "area_code": "JP-27-OSAKA-KITA",
      "area_name": "Osaka Kita",
      "center_latitude": 34.7052,
      "center_longitude": 135.4982,
      "amount_minor": 210000,
      "expense_count": 9
    },
    {
      "area_code": "JP-27-OSAKA-NANIWA",
      "area_name": "Osaka Naniwa",
      "center_latitude": 34.6665,
      "center_longitude": 135.5000,
      "amount_minor": 84000,
      "expense_count": 4
    }
  ]
}
```

## 11.5 `GET /v1/trips/{trip_id}/map`

지도 복원에 필요한 경로와 마커를 한 번에 내려주는 endpoint

### Response

```json
{
  "data": {
    "trip": {
      "trip_id": "trip_123",
      "title": "오사카 3박 4일"
    },
    "route_points": [
      { "latitude": 34.70241, "longitude": 135.49592, "captured_at": "2026-05-03T03:14:58Z" },
      { "latitude": 34.69900, "longitude": 135.50211, "captured_at": "2026-05-03T04:05:12Z" }
    ],
    "markers": [
      {
        "expense_id": "exp_123",
        "amount_minor": 12800,
        "place_name": "Starbucks Umeda",
        "latitude": 34.70241,
        "longitude": 135.49592,
        "occurred_at": "2026-05-03T03:15:21Z"
      }
    ]
  }
}
```

## 12. 동기화 API 설계 원칙

### 12.1 idempotency

모바일은 네트워크 불안정 때문에 같은 요청을 재전송할 수 있다.

따라서:

- `client_id`는 반드시 unique 해야 한다.
- 서버는 `client_id` 기준 upsert 해야 한다.

### 12.2 last-write-wins

수정 충돌이 생기면 기본적으로 `updated_at`이 더 최신인 쪽을 우선한다.

### 12.3 soft delete

- 삭제는 즉시 물리 삭제하지 않는다.
- `deleted = true` 로 처리한다.

## 13. Places 보정 API

MVP에서 꼭 필요하지는 않지만, 지도 품질 향상을 위해 추가 가능하다.

## 13.1 `POST /v1/places/resolve`

### Request

```json
{
  "trip_id": "trip_123",
  "merchant_name": "STARBUCKS",
  "latitude": 34.70241,
  "longitude": 135.49592
}
```

### Response

```json
{
  "data": {
    "place_id": "google-place-id",
    "place_name": "Starbucks Umeda",
    "address_text": "1-1 Umeda, Osaka",
    "area_code": "JP-27-OSAKA-KITA",
    "area_name": "Osaka Kita",
    "confidence": 0.88
  }
}
```

## 14. 추천 서버 테이블 개요

- `users`
- `devices`
- `trips`
- `expenses`
- `location_samples`
- `expense_region_aggregates`
- `trip_daily_stats`

## 15. 에러 코드 예시

- `UNAUTHORIZED`
- `TRIP_NOT_FOUND`
- `EXPENSE_NOT_FOUND`
- `LOCATION_NOT_FOUND`
- `VALIDATION_ERROR`
- `RATE_LIMITED`
- `CONFLICT_CLIENT_ID`
- `INTERNAL_SERVER_ERROR`

## 16. Android 클라이언트 구현 포인트

- 서버 ID와 로컬 ID를 모두 보관한다.
- 업로드 성공 시 Room의 `serverId`, `syncStatus`, `lastSyncedAt`를 갱신한다.
- 실패 시 `SyncQueueEntity`의 retryCount 와 nextRetryAt 를 갱신한다.
- `GET /map`, `GET /stats/*`는 앱 재설치 또는 멀티 디바이스 복원에 유용하다.

## 17. MVP 우선순위

### 우선 구현

- `GET /me`
- `POST /devices`
- `POST /trips`
- `PATCH /trips/{trip_id}`
- `POST /trips/{trip_id}/start`
- `POST /trips/{trip_id}/finish`
- `POST /trips/{trip_id}/expenses/batch`
- `POST /trips/{trip_id}/locations/batch`
- `GET /trips/{trip_id}/map`
- `GET /trips/{trip_id}/stats/overview`
- `GET /trips/{trip_id}/stats/categories`
- `GET /trips/{trip_id}/stats/regions`

### 나중 구현

- `POST /places/resolve`
- 복잡한 conflict resolution
- 광고/추천 관련 API
