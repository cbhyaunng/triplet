# Triplet iOS

SwiftUI + MapKit 기반의 Triplet 발표용 iOS 앱입니다.

## 실행 방법

1. Xcode에서 `TripletIOS.xcodeproj`를 엽니다.
2. 실행 대상 시뮬레이터를 선택합니다.
3. `TripletIOS` scheme 을 실행합니다.

터미널 빌드:

```bash
cd ios/TripletIOS
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild \
  -project TripletIOS.xcodeproj \
  -scheme TripletIOS \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  build
```

## 시연 흐름

1. 홈에서 `여행 시작`을 누릅니다.
2. 홈의 `시연 결제 알림` 카드에서 `알림 보내기`를 누릅니다.
3. 빠른 템플릿을 선택하거나 가게명, 금액, 좌표를 직접 입력합니다.
4. `알림 보내기`를 누르면 Triplet 앱 내부 로컬 알림이 발송됩니다.
5. 앱이 알림 payload 를 읽어 소비내역과 지도 핀에 반영합니다.
6. `여행 중지`를 누르면 현재 여행이 `여행관리`에 저장됩니다.

## iOS 시연 방식

iOS는 다른 앱의 알림을 읽을 수 없으므로, 이 앱은 Triplet 내부에서 만든 로컬 알림의 `userInfo` payload 를 파싱하는 방식으로 시연합니다.

알림 payload 에 좌표가 포함되어 있으면 GPS 샘플 없이도 지도에 결제 핀이 표시됩니다.
