# TRIPLET Notification Format Spec

## 1. 목적

이 문서는 발표용 테스트 알림 앱이 보내는 결제 알림의 포맷을 정의한다.

트립랫은 이 규격을 기준으로 알림을 파싱한다.

## 2. 설계 원칙

- 사람에게는 결제 알림처럼 자연스럽게 보인다.
- 앱에는 안정적으로 읽히는 구조화 데이터가 들어간다.
- 포맷 버전 관리가 가능해야 한다.
- title/text 만으로도 최소 fallback 파싱이 가능해야 한다.

## 3. 전송 대상

- sender app package: `com.triplet.demo.notifier`
- listener app package: `com.triplet`

## 4. Notification 기본 규격

### 채널

- channel id: `triplet_demo_payment_alerts`
- channel name: `Demo Payment Alerts`
- importance: `IMPORTANCE_HIGH`

### 분류

- subText: `TRIPLET_DEMO_V1`
- group key: `triplet_demo_payment_group`

## 5. payload 버전

### 현재 버전

- `version = 1`

### 버전 필드 위치

- custom extras key: `triplet.demo.version`
- 보조 마커: `subText = TRIPLET_DEMO_V1`

listener 는 먼저 extras version 을 확인하고, 없으면 subText 를 fallback 으로 본다.

## 6. 알림에 포함할 데이터

## 6.1 필수 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `tx_id` | String | 거래 고유 ID. 중복 방지용 |
| `merchant` | String | 가맹점명 |
| `amount_minor` | Long | 소수점 없는 최소 화폐 단위 금액 |
| `currency` | String | ISO 4217 통화 코드 |
| `occurred_at` | String | ISO-8601 시각 |

## 6.2 선택 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `category` | String | FOOD, CAFE, TRANSPORT 등 |
| `card_label` | String | 테스트 카드 이름 |
| `note` | String | 메모 |
| `country_code` | String | KR, JP 등 |

## 7. custom extras 규격

테스트 앱은 아래 키를 Notification extras 에 넣는다.

| extras key | 예시 값 |
|---|---|
| `triplet.demo.version` | `1` |
| `triplet.demo.tx_id` | `demo-20260425-0001` |
| `triplet.demo.merchant` | `스타벅스` |
| `triplet.demo.amount_minor` | `12800` |
| `triplet.demo.currency` | `KRW` |
| `triplet.demo.occurred_at` | `2026-04-25T14:31:00+09:00` |
| `triplet.demo.category` | `CAFE` |
| `triplet.demo.card_label` | `TRIPLET TEST CARD` |
| `triplet.demo.note` | `오사카역 지점` |

## 8. 시각적으로 보이는 알림 포맷

사람이 보는 title/text 는 아래 규칙을 따른다.

### contentTitle

```text
[승인] 12,800 KRW
```

### contentText

```text
스타벅스 · 2026-04-25T14:31:00+09:00
```

### subText

```text
TRIPLET_DEMO_V1
```

### bigText

fallback 파싱을 위해 key=value 형태의 문자열을 넣는다.

```text
merchant=스타벅스; amount_minor=12800; currency=KRW; occurred_at=2026-04-25T14:31:00+09:00; category=CAFE; tx_id=demo-20260425-0001
```

## 9. listener 파싱 우선순위

트립랫은 아래 순서로 읽는다.

1. custom extras
2. bigText 의 key=value 문자열
3. contentTitle/contentText fallback

즉, 화면에 보이는 문자열은 시연용이고, 실제 parsing 은 extras 중심으로 설계한다.

## 10. 예시 payload

### 예시 1. 카페 결제

```json
{
  "triplet.demo.version": 1,
  "triplet.demo.tx_id": "demo-20260425-0001",
  "triplet.demo.merchant": "스타벅스",
  "triplet.demo.amount_minor": 12800,
  "triplet.demo.currency": "KRW",
  "triplet.demo.occurred_at": "2026-04-25T14:31:00+09:00",
  "triplet.demo.category": "CAFE",
  "triplet.demo.card_label": "TRIPLET TEST CARD",
  "triplet.demo.note": "오사카역 지점"
}
```

### 예시 2. 교통 결제

```json
{
  "triplet.demo.version": 1,
  "triplet.demo.tx_id": "demo-20260425-0002",
  "triplet.demo.merchant": "공항철도",
  "triplet.demo.amount_minor": 4450,
  "triplet.demo.currency": "KRW",
  "triplet.demo.occurred_at": "2026-04-25T14:45:00+09:00",
  "triplet.demo.category": "TRANSPORT"
}
```

## 11. tx_id 생성 규칙

중복 처리 방지를 위해 tx_id 는 유일해야 한다.

### 권장 포맷

```text
demo-YYYYMMDD-HHMMSS-#### 
```

예:

```text
demo-20260425-143100-0001
```

## 12. 금액 규칙

- 화면 표시와 별개로 parser 에는 `amount_minor` 를 정수로 넣는다.
- KRW 는 `12800`
- JPY 는 `5400`

발표용 데모에서는 환율 계산 없이 `표시 통화`와 `원본 통화`를 동일하게 두는 편이 구현이 단순하다.

## 13. 시각 규칙

- `occurred_at` 은 반드시 ISO-8601 문자열 사용
- 타임존 포함 권장

### 예시

```text
2026-04-25T14:31:00+09:00
```

## 14. 구현 예시

NotificationCompat.Builder 는 extras 를 넣을 수 있으므로, sender 앱은 custom metadata 를 함께 넣는다.

```kotlin
val extras = bundleOf(
    "triplet.demo.version" to 1,
    "triplet.demo.tx_id" to txId,
    "triplet.demo.merchant" to merchant,
    "triplet.demo.amount_minor" to amountMinor,
    "triplet.demo.currency" to currency,
    "triplet.demo.occurred_at" to occurredAt,
    "triplet.demo.category" to category
)

val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle("[승인] $amountMinor $currency")
    .setContentText("$merchant · $occurredAt")
    .setSubText("TRIPLET_DEMO_V1")
    .addExtras(extras)
    .build()
```

## 15. validation 규칙

listener 는 아래를 검증한다.

- `version == 1`
- `tx_id` not blank
- `merchant` not blank
- `amount_minor > 0`
- `currency.length == 3`
- `occurred_at` 파싱 가능

검증 실패 시 `PARSE_FAILED` 로 저장하고 디버그 로그에 이유를 남긴다.

## 16. 포맷 변경 규칙

포맷을 바꾸려면 아래를 같이 변경한다.

1. sender app 의 extras key 또는 값
2. listener parser version
3. subText marker
4. 테스트 시나리오 문서

하위 호환이 필요하면 `DemoPaymentNotificationParserV1`, `V2`처럼 parser 를 분리한다.
