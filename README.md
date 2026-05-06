# Triplet

Triplet은 여행 중 발생한 결제 내역과 이동 경로를 지도 위에 함께 보여주는 **여행 소비 지도 앱**입니다. 사용자가 여행모드를 켜면 위치 흐름을 기록하고, 결제 알림 또는 직접 입력한 결제 데이터를 위치와 매칭해 “어디서, 언제, 얼마를 썼는지”를 직관적으로 확인할 수 있습니다.

Android는 테스트 결제 알림 발송 앱과 연동해 알림 수신 흐름을 검증하고, iOS는 앱 내부 로컬 결제 알림과 데모 데이터를 통해 동일한 소비 기록 경험을 제공합니다.

## 핵심 컨셉

- 여행 중 결제 내역을 지도 위 핀으로 표시
- 결제 발생 순서대로 이동 경로를 선으로 연결
- 소비 내역, 소비 요약, 카테고리별 지출 비중 제공
- 여행모드를 기준으로 위치 수집 시작/종료
- 데모 데이터와 테스트 결제 알림 제공

## 주요 기능

- 여행모드 ON/OFF
- 지도 위 이동 경로와 소비 마커 표시
- 핀 선택 시 결제 장소와 금액 확인
- 최근 소비 내역과 전체 소비 내역 화면
- 수동 소비 입력
- 카테고리별 소비 요약
- 도넛형 지출 그래프
- 소비 패턴에 따른 “합리적인 소비를 위한 코멘트”
- 지난 여행 기록 저장 및 조회
- 데모 데이터 입력

## iOS 앱

iOS 버전은 SwiftUI 기반 앱입니다. iOS에서는 다른 앱의 카드사 알림을 직접 읽을 수 없기 때문에, Triplet 앱 내부에서 생성한 로컬 결제 알림 payload 를 파싱하거나 설정 화면의 데모 데이터를 입력하는 방식으로 소비 기록을 생성합니다.

### iOS 주요 기능

- SwiftUI 기반 Toss 스타일 UI
- MapKit 지도 표시
- CoreLocation 위치 수집
- 앱 내부 로컬 결제 알림 처리
- 설정 화면의 데모 데이터 입력
- Live Activity 기반 잠금화면/알림센터 여행모드 표시
- iOS 18 이상 Control Widget 기반 제어센터 여행모드 토글
- App Group 공유 상태를 통한 앱과 제어센터 동기화

### iOS 실행

```bash
cd ios/TripletIOS
xcodegen generate
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild \
  -project TripletIOS.xcodeproj \
  -scheme TripletIOS \
  -destination 'platform=iOS Simulator,name=Triplet iPhone 13 Pro' \
  build
```

실기기에서 제어센터 여행모드 토글을 확인하려면 Apple Developer 계정에서 App Group `group.com.triplet.ios` 권한을 활성화해야 합니다. iPhone 13 Pro는 Dynamic Island가 없으므로 Live Activity는 잠금화면/알림센터에서 확인합니다.

## Android 앱

Android 버전은 `NotificationListenerService`를 이용해 테스트 알림 발송 앱의 결제 알림을 읽고, 결제 시점의 위치와 매칭하는 구조입니다. 실제 카드사 앱이나 SMS 직접 읽기는 현재 구현 범위에서 제외했습니다.

### Android 주요 기능

- Jetpack Compose 기반 UI
- Google Maps SDK 지도 표시
- Fused Location Provider 위치 수집
- 테스트 결제 알림 수신
- 결제 시점과 위치 자동 매칭
- Room 기반 로컬 저장 구조 설계
- 데모 알림 발송 앱 `demo-notifier`

### Android 실행 준비

Android SDK 경로와 Google Maps API 키를 `local.properties`에 설정합니다.

```properties
sdk.dir=/Users/changminbyun/Library/Android/sdk
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

`local.properties`는 개인 환경 파일이므로 Git에 올리지 않습니다. 예시는 `local.properties.example`을 참고하면 됩니다.

### Android 빌드

```bash
./gradlew :app:assembleDebug :demo-notifier:assembleDebug
```

## 프로젝트 구성

- `app`: Android Triplet 메인 앱
- `demo-notifier`: Android 테스트 결제 알림 발송 앱
- `ios/TripletIOS`: iOS SwiftUI Triplet 앱
- `docs`: 아키텍처, 권한 흐름, API 명세, 알림 포맷 등 설계 문서
- `preview.md`: 초기 기획/설명 자료

## 문서

자세한 설계 문서는 [docs/README.md](./docs/README.md)에 정리되어 있습니다.

## 주의사항

- Android는 실제 카드사 앱 대신 테스트 알림 앱을 사용합니다.
- iOS는 운영체제 제약으로 다른 앱 알림을 읽지 않고, 앱 내부 로컬 알림을 사용합니다.
- Google Maps API 키와 개인 로컬 설정은 저장소에 올리지 않습니다.
