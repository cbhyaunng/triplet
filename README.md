# Triplet

Triplet은 여행 중 발생한 결제 내역과 이동 경로를 지도 위에 함께 보여주는 안드로이드 데모 앱입니다. 사용자가 여행모드를 켜면 위치를 기록하고, 테스트용 결제 알림 앱에서 발생한 결제 알림을 읽어 가장 가까운 위치와 매칭합니다.

이 프로젝트는 대학 수업 발표용 MVP이며, 실제 카드사 앱이나 SMS를 직접 읽지 않고 `demo-notifier` 모듈이 보내는 테스트 결제 알림을 사용합니다.

## 주요 기능

- 여행모드 ON/OFF
- GPS 위치 샘플 수집
- 테스트 결제 알림 수신
- 결제 시점과 위치 자동 매칭
- 지도 위 이동 경로와 소비 마커 표시
- 지도 전체 화면 보기
- 지도 핀 선택 시 결제 장소와 금액 표시
- 소비 요약과 카테고리별 지출 표시
- 최근 소비 내역 2개 미리보기
- 전체 소비 내역 화면
- 발표용 5개 방문지 데모 데이터 주입

## 프로젝트 구성

- `app`: Triplet 메인 앱
- `demo-notifier`: 발표용 테스트 결제 알림 발송 앱
- `docs`: 설계 문서, 권한 흐름, API 명세, 알림 포맷 규격

## 기술 스택

- Kotlin
- Jetpack Compose
- Google Maps SDK for Android
- Fused Location Provider
- NotificationListenerService
- Gradle Kotlin DSL

## 실행 준비

Android SDK 경로와 Google Maps API 키를 `local.properties`에 설정합니다.

```properties
sdk.dir=/Users/changminbyun/Library/Android/sdk
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

`local.properties`는 개인 환경 파일이므로 Git에 올리지 않습니다. 예시는 `local.properties.example`을 참고하면 됩니다.

## 빌드

```bash
./gradlew :app:assembleDebug :demo-notifier:assembleDebug
```

빌드가 성공하면 다음 위치에 APK가 생성됩니다.

- `app/build/outputs/apk/debug/app-debug.apk`
- `demo-notifier/build/outputs/apk/debug/demo-notifier-debug.apk`

## 데모 시나리오

1. Android Studio에서 Pixel 8 등 에뮬레이터를 실행합니다.
2. `app`과 `demo-notifier`를 설치합니다.
3. Triplet 앱에서 알림 접근 권한과 위치 권한을 허용합니다.
4. Triplet에서 `여행 시작`을 누릅니다.
5. `데모 경로`를 누르면 5개 방문지와 30~60분 간격 결제 데이터가 생성됩니다.
6. `지도 위 소비 기록` 카드를 누르면 전체 지도 화면으로 이동합니다.
7. 지도 핀을 누르면 하단 창에서 결제 장소와 금액을 확인할 수 있습니다.
8. 소비 내역의 `더보기`를 누르면 전체 소비 내역 화면으로 이동합니다.

## 데모 데이터

`데모 경로` 버튼은 기존 위치/결제 데이터를 정리한 뒤 서울 5개 방문지 데이터를 생성합니다. 각 결제는 30~60분 간격으로 발생한 것처럼 저장되며, 지도에서는 결제 발생 순서대로 파란 선이 연결됩니다.

## 문서

자세한 설계 문서는 [docs/README.md](./docs/README.md)에 정리되어 있습니다.

## 주의사항

- 이 앱은 발표용 MVP입니다.
- 실제 카드사 앱 연동 대신 테스트 알림 앱을 사용합니다.
- SMS 직접 읽기는 구현하지 않았습니다.
- Google Maps API 키는 개인 키이므로 저장소에 올리지 않습니다.
