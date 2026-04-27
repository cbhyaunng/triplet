# TRIPLET Demo Notification App

## 1. 목적

이 문서는 발표용 데모를 위해 사용하는 `테스트 알림 발송 앱`의 설계를 정의한다.

이 앱의 역할은 실제 카드사 앱을 대체해 결제 승인처럼 보이는 알림을 발생시키는 것이다.

트립랫은 이 앱의 알림을 NotificationListenerService 로 읽어와 지출 내역으로 변환한다.

## 2. 왜 별도 앱으로 분리하는가

- 실제 카드사 앱 없이도 결제 알림 흐름을 시연할 수 있다.
- 발표 중 알림 내용을 완전히 통제할 수 있다.
- 지원 카드사별 포맷 차이 없이 일관된 parser 를 만들 수 있다.
- 트립랫 앱 내부에서 fake 이벤트를 직접 만드는 것보다 실제 안드로이드 알림 흐름에 가깝다.

## 3. 앱 이름과 기본 식별자

### 제안 이름

- 사용자 표시 이름: `Triplet Test Notifier`
- 패키지명: `com.triplet.demo.notifier`

### 고정 식별자

- notification channel id: `triplet_demo_payment_alerts`
- notification group key: `triplet_demo_payment_group`
- format version marker: `TRIPLET_DEMO_V1`

트립랫 listener 의 allowlist 는 기본적으로 이 패키지명만 읽도록 설정한다.

## 4. 최소 기능 범위

### 필수 기능

- 즉시 결제 알림 1건 발송
- 템플릿 기반 결제 알림 발송
- 금액, 가맹점명, 시각, 카테고리 입력
- 거래 ID 자동 생성
- 최근 발송 이력 보기

### 있으면 좋은 기능

- 3건 연속 데모 발송
- 랜덤 결제 샘플 발송
- 지연 발송 3초, 5초 타이머
- 카테고리 프리셋 버튼

## 5. 권장 화면 구성

## 5.1 SenderScreen

발표 중 가장 많이 쓰는 메인 화면이다.

### 입력 항목

- 가맹점명
- 금액
- 통화
- 카테고리
- 결제 시각
- 노트

### 버튼

- `지금 발송`
- `3초 후 발송`
- `초기화`

## 5.2 TemplateScreen

자주 쓰는 시나리오를 빠르게 불러오기 위한 화면

### 기본 템플릿 예시

- 스타벅스 12,800원
- 세븐일레븐 5,000원
- 공항철도 4,450원
- 이치란라멘 22,000원
- 돈키호테 76,400원

## 5.3 HistoryScreen

최근 발송한 알림 이력을 보여준다.

### 표시 항목

- 거래 ID
- 가맹점명
- 금액
- 발송 시각
- 재발송 버튼

## 6. 내부 아키텍처

이 앱은 간단한 단일 앱 구조로 충분하다.

```text
demo-notifier/
  src/main/java/com/triplet/demo/notifier/
    MainActivity.kt
    feature/
      sender/
        SenderScreen.kt
        SenderViewModel.kt
      template/
        TemplateScreen.kt
      history/
        HistoryScreen.kt
    notification/
      DemoNotificationChannel.kt
      DemoPaymentNotificationFactory.kt
      DemoNotificationSender.kt
    model/
      DemoPaymentPayload.kt
      DemoPaymentTemplate.kt
```

## 7. 핵심 클래스 역할

### `DemoPaymentPayload`

발송할 결제 알림의 구조화된 모델

추천 필드:

- `txId`
- `merchantName`
- `amountMinor`
- `currencyCode`
- `category`
- `occurredAt`
- `note`

### `DemoPaymentNotificationFactory`

payload 를 받아 NotificationCompat.Builder 로 변환한다.

역할:

- title, text, subText 생성
- custom extras 삽입
- BigTextStyle 설정
- channel id 지정

### `DemoNotificationSender`

NotificationManagerCompat 를 통해 실제 알림을 발송한다.

역할:

- channel 존재 보장
- notification id 생성
- 즉시 발송
- 지연 발송 스케줄링

## 8. 알림 생성 방식

발표용 앱은 `보이는 텍스트`와 `구조화된 extras`를 함께 넣는 방식을 추천한다.

이유:

- 화면에서는 실제 결제 알림처럼 자연스럽게 보인다.
- 트립랫 parser 는 extras 를 읽어 안정적으로 파싱할 수 있다.
- extras 가 없거나 손상된 경우 title/text 로 fallback 가능하다.

## 9. 권장 Notification 생성 예시

```kotlin
val extras = bundleOf(
    "triplet.demo.version" to 1,
    "triplet.demo.tx_id" to payload.txId,
    "triplet.demo.merchant" to payload.merchantName,
    "triplet.demo.amount_minor" to payload.amountMinor,
    "triplet.demo.currency" to payload.currencyCode,
    "triplet.demo.category" to payload.category.name,
    "triplet.demo.occurred_at" to payload.occurredAt.toString(),
    "triplet.demo.note" to payload.note
)

val notification = NotificationCompat.Builder(context, DEMO_CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_demo_card)
    .setContentTitle("[승인] ${payload.amountMinor} ${payload.currencyCode}")
    .setContentText("${payload.merchantName} · ${payload.occurredAt}")
    .setSubText("TRIPLET_DEMO_V1")
    .setStyle(
        NotificationCompat.BigTextStyle()
            .bigText("merchant=${payload.merchantName}; amount_minor=${payload.amountMinor}; currency=${payload.currencyCode}; occurred_at=${payload.occurredAt}; category=${payload.category.name}; tx_id=${payload.txId}")
    )
    .addExtras(extras)
    .setAutoCancel(true)
    .build()
```

## 10. Android 권한 및 설정

### 필요한 것

- Android 13 이상에서는 `POST_NOTIFICATIONS`
- Notification Channel 생성

### 필요 없는 것

- 위치 권한
- 알림 listener 권한
- 서버 연동

## 11. 발표 시연 추천 시나리오

### 시나리오 A. 단건 승인

1. Triplet 에서 여행모드 ON
2. Test Notifier 에서 스타벅스 템플릿 선택
3. `지금 발송`
4. Triplet 홈 지도에 마커 생성 확인

### 시나리오 B. 연속 소비

1. 편의점, 카페, 교통 3건을 순차 발송
2. Triplet 내역 리스트 증가 확인
3. 지도 경로와 소비 마커 표시 확인
4. 통계 화면에서 카테고리 비율 확인

## 12. QA 체크리스트

- 앱 설치 직후 알림 권한 요청이 정상 동작하는가
- 채널이 없을 때 자동 생성되는가
- tx_id 가 중복되지 않는가
- contentTitle, contentText, extras 값이 listener 기대값과 일치하는가
- BigText 에 fallback 파싱용 문자열이 들어가는가

## 13. 구현 우선순위

1. 단건 발송
2. 템플릿 발송
3. 발송 이력
4. 지연 발송
5. 연속 데모 발송
