# TRIPLET Android Docs

트립랫 안드로이드 앱 구현을 위한 기준 문서 모음입니다.

## 문서 목록

- [TRIPLET_ANDROID_ARCHITECTURE.md](./TRIPLET_ANDROID_ARCHITECTURE.md)
  - 제품 정의, MVP 범위, 핵심 흐름, 기술 스택, 안드로이드 프로젝트 폴더 구조
- [TRIPLET_ROOM_SCHEMA.md](./TRIPLET_ROOM_SCHEMA.md)
  - Room 엔티티, 관계, 인덱스, DAO 역할, 로컬 데이터 흐름
- [TRIPLET_PERMISSION_FLOW.md](./TRIPLET_PERMISSION_FLOW.md)
  - 화면별 권한 요청 시나리오, 여행모드 기준 동작, 예외 처리
- [TRIPLET_SERVER_API.md](./TRIPLET_SERVER_API.md)
  - 서버 API 명세, 동기화 방식, 요청/응답 예시, 에러 포맷
- [TRIPLET_DEMO_NOTIFICATION_APP.md](./TRIPLET_DEMO_NOTIFICATION_APP.md)
  - 발표용 테스트 알림 발송 앱 설계, 화면 구성, 발송 흐름, Notification 생성 방식
- [TRIPLET_LISTENER_CODE_STRUCTURE.md](./TRIPLET_LISTENER_CODE_STRUCTURE.md)
  - 트립랫의 NotificationListenerService 내부 구조, 클래스 책임, 처리 파이프라인
- [TRIPLET_NOTIFICATION_FORMAT_SPEC.md](./TRIPLET_NOTIFICATION_FORMAT_SPEC.md)
  - 테스트용 결제 알림 포맷 규격, 커스텀 extras, fallback 파싱 규칙
- [TRIPLET_IMPLEMENTATION_CHECKLIST.md](./TRIPLET_IMPLEMENTATION_CHECKLIST.md)
  - 발표용 데모 구현 순서, 테스트 알림 앱 우선 개발 체크리스트, 단계별 완료 기준

## 권장 읽는 순서

1. Android Architecture
2. Room Schema
3. Permission Flow
4. Server API
5. Demo Notification App
6. Listener Code Structure
7. Notification Format Spec
8. Implementation Checklist

## 이번 문서에서 고정한 전제

- 플랫폼은 안드로이드만 고려한다.
- 핵심 기능은 `여행모드 + 결제 알림 수집 + 위치 매칭 + 지도 가계부`다.
- 발표용 데모 빌드는 `테스트용 알림 발송 앱`이 생성한 결제 알림을 읽는다.
- listener 구조는 이후 실제 카드사/은행 앱 parser 로 확장할 수 있게 유지한다.
- Google Play 정책 리스크 때문에 `SMS 직접 읽기`는 MVP에서 제외한다.
- 앱은 `local-first` 구조로 동작하고, 로컬 저장 후 서버로 동기화한다.
- 위치 추적은 사용자가 앱 안에서 여행모드를 켤 때 시작한다.
- MVP에서는 `ACCESS_BACKGROUND_LOCATION`을 기본 요구사항으로 두지 않는다.
- 자동 매칭이 애매한 경우를 위해 `수동 수정`과 `수동 입력`을 반드시 제공한다.
- 서버에는 구조화된 결제/위치 데이터를 저장하되, 카드 알림 원문 전체는 장기 보관하지 않는다.

## MVP 범위

- 여행 생성/시작/종료
- 여행모드 ON/OFF
- 알림 접근 권한 기반 결제 알림 수집
- 위치 수집 및 결제 시점 매칭
- 지도 위 경로/마커 표시
- 지출 리스트/가계부
- 카테고리별 통계
- 지역별 소비 집중도
- 서버 동기화

## MVP 제외 항목

- iOS 앱
- SMS 직접 읽기
- AI 여행 경로 추천
- 주변 광고 푸시
- 모든 카드사/은행 완전 지원
- 실시간 다중 디바이스 협업
