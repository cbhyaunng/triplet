import SwiftUI

struct TripletSettingsToolbar: ViewModifier {
    let notificationService: DemoPaymentNotificationService
    @State private var settingsPresented = false

    func body(content: Content) -> some View {
        content
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        settingsPresented = true
                    } label: {
                        Image(systemName: "gearshape.fill")
                            .font(.headline.weight(.bold))
                    }
                    .tint(TripletColor.grey900)
                    .accessibilityLabel("설정")
                }
            }
            .sheet(isPresented: $settingsPresented) {
                SettingsPage(notificationService: notificationService)
            }
    }
}

extension View {
    func tripletSettingsToolbar(notificationService: DemoPaymentNotificationService) -> some View {
        modifier(TripletSettingsToolbar(notificationService: notificationService))
    }
}

struct ContentView: View {
    @EnvironmentObject private var store: TripletStore
    @EnvironmentObject private var locationService: TravelLocationService
    let notificationService: DemoPaymentNotificationService

    var body: some View {
        TabView(selection: $store.selectedTab) {
            HomeView(notificationService: notificationService)
                .tabItem {
                    Label(TripletTab.home.title, systemImage: TripletTab.home.systemImage)
                }
                .tag(TripletTab.home)

            CurrentMapPage(notificationService: notificationService)
                .tabItem {
                    Label(TripletTab.map.title, systemImage: TripletTab.map.systemImage)
                }
                .tag(TripletTab.map)

            ExpensesPage(notificationService: notificationService)
                .tabItem {
                    Label(TripletTab.expenses.title, systemImage: TripletTab.expenses.systemImage)
                }
                .tag(TripletTab.expenses)

            SummaryPage(notificationService: notificationService)
                .tabItem {
                    Label(TripletTab.summary.title, systemImage: TripletTab.summary.systemImage)
                }
                .tag(TripletTab.summary)

            TripManagementPage(notificationService: notificationService)
                .tabItem {
                    Label(TripletTab.trips.title, systemImage: TripletTab.trips.systemImage)
                }
                .tag(TripletTab.trips)
        }
        .tint(TripletColor.grey900)
    }
}

struct HomeView: View {
    @EnvironmentObject private var store: TripletStore
    @EnvironmentObject private var locationService: TravelLocationService
    let notificationService: DemoPaymentNotificationService

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    HeroCard()

                    TripletCard {
                        SectionHeader(
                            title: "소비 지도",
                            subtitle: "결제 위치와 이동 경로",
                            actionTitle: "지도 보기"
                        ) {
                            store.selectedTab = .map
                        }
                        TripMapView(
                            expenses: store.expenses,
                            samples: store.locationSamples,
                            height: 320
                        )
                    }

                    SectionHeader(
                        title: "소비 내역",
                        subtitle: "최근 결제순",
                        actionTitle: store.expenses.count > 2 ? "더보기" : nil
                    ) {
                        store.selectedTab = .expenses
                    }
                    if store.expenses.isEmpty {
                        EmptyExpenseCard()
                    } else {
                        ForEach(store.sortedExpenses.prefix(2)) { expense in
                            ExpenseRow(expense: expense)
                        }
                    }

                    SummaryCard()
                }
                .padding(18)
            }
            .background(TripletColor.grey100)
            .navigationTitle("Triplet")
            .tripletSettingsToolbar(notificationService: notificationService)
        }
    }
}

struct HeroCard: View {
    @EnvironmentObject private var store: TripletStore
    @EnvironmentObject private var locationService: TravelLocationService

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("이번 여행 소비")
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(TripletColor.grey700)
                    Text(TripletFormat.won(store.totalAmountMinor))
                        .font(.system(size: 34, weight: .black, design: .rounded))
                        .foregroundStyle(TripletColor.grey900)
                    Text("결제 \(store.expenses.count)건")
                        .font(.body)
                        .foregroundStyle(TripletColor.grey500)
                }
                Spacer()
                InfoPill(
                    text: store.travelModeEnabled ? "여행 중" : "대기",
                    background: store.travelModeEnabled ? TripletColor.blue50 : TripletColor.grey100,
                    foreground: store.travelModeEnabled ? TripletColor.blueDark : TripletColor.grey600
                )
            }

            Button {
                if store.travelModeEnabled {
                    locationService.stopTracking()
                    _ = store.stopTrip()
                } else {
                    store.startTrip()
                    locationService.requestWhenInUse()
                    locationService.startTracking()
                }
            } label: {
                Label(
                    store.travelModeEnabled ? "여행 중지" : "여행 시작",
                    systemImage: store.travelModeEnabled ? "stop.fill" : "play.fill"
                )
                .frame(maxWidth: .infinity)
                .font(.headline.weight(.bold))
            }
            .buttonStyle(.borderedProminent)
            .tint(store.travelModeEnabled ? TripletColor.grey900 : TripletColor.blue)
            .controlSize(.large)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

struct CurrentMapPage: View {
    @EnvironmentObject private var store: TripletStore
    let notificationService: DemoPaymentNotificationService

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    TripletCard {
                        SectionHeader(
                            title: "소비 지도",
                            subtitle: "결제 순서대로 선을 이어 보여줍니다."
                        )
                        HStack {
                            InfoPill(text: "경로 \(store.locationSamples.count)", background: TripletColor.blue50, foreground: TripletColor.blueDark)
                            InfoPill(text: "소비 \(store.expenses.count)", background: TripletColor.orange50, foreground: TripletColor.orange)
                        }
                        TripMapView(
                            expenses: store.expenses,
                            samples: store.locationSamples,
                            height: 540
                        )
                    }
                }
                .padding(18)
            }
            .background(TripletColor.grey100)
            .navigationTitle("지도")
            .tripletSettingsToolbar(notificationService: notificationService)
        }
    }
}

struct ExpensesPage: View {
    @EnvironmentObject private var store: TripletStore
    let notificationService: DemoPaymentNotificationService
    @State private var manualInputPresented = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    SectionHeader(
                        title: "소비 내역",
                        subtitle: "전체 \(store.expenses.count)건",
                        actionTitle: "수동 입력"
                    ) {
                        manualInputPresented = true
                    }
                    if store.expenses.isEmpty {
                        EmptyExpenseCard()
                    } else {
                        ForEach(store.sortedExpenses) { expense in
                            ExpenseRow(expense: expense)
                        }
                    }
                }
                .padding(18)
            }
            .background(TripletColor.grey100)
            .navigationTitle("소비 내역")
            .tripletSettingsToolbar(notificationService: notificationService)
            .sheet(isPresented: $manualInputPresented) {
                ManualExpenseEntryView()
            }
        }
    }
}

struct SummaryPage: View {
    let notificationService: DemoPaymentNotificationService

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    SummaryCard()
                }
                .padding(18)
            }
            .background(TripletColor.grey100)
            .navigationTitle("소비 요약")
            .tripletSettingsToolbar(notificationService: notificationService)
        }
    }
}

struct SettingsPage: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: TripletStore
    let notificationService: DemoPaymentNotificationService
    @State private var statusMessage: String?
    @State private var demoFormPresented = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    TripletCard {
                        SectionHeader(
                            title: "데모 데이터",
                            subtitle: "발표 시연용 결제와 경로"
                        )
                        Text("결제 5건과 경로 6점을 현재 여행에 입력합니다. 입력 후 홈, 지도, 소비내역, 소비요약 탭에서 바로 확인할 수 있습니다.")
                            .font(.subheadline)
                            .foregroundStyle(TripletColor.grey600)

                        HStack {
                            SummaryMetric(label: "결제", value: "5건")
                            SummaryMetric(label: "경로", value: "6점")
                            SummaryMetric(label: "장소", value: "서울")
                        }

                        Button {
                            store.insertDemoData()
                            statusMessage = "데모 데이터가 입력되었습니다."
                        } label: {
                            Label("데모 데이터 입력", systemImage: "sparkles")
                                .frame(maxWidth: .infinity)
                                .font(.headline.weight(.bold))
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(TripletColor.blue)
                        .controlSize(.large)

                        Button(role: .destructive) {
                            store.clearCurrentData()
                            statusMessage = "현재 여행 데이터가 초기화되었습니다."
                        } label: {
                            Label("현재 여행 데이터 초기화", systemImage: "trash")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.large)

                        if let statusMessage {
                            Text(statusMessage)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(TripletColor.grey600)
                        }
                    }

                    TripletCard {
                        SectionHeader(
                            title: "시연 결제 알림",
                            subtitle: "앱 내부 로컬 알림으로 결제를 추가합니다.",
                            actionTitle: "알림 보내기"
                        ) {
                            demoFormPresented = true
                        }
                        Text("iOS에서는 다른 앱 알림을 읽을 수 없어, 발표 시연용으로 Triplet 앱이 만든 알림 payload 를 파싱합니다.")
                            .font(.subheadline)
                            .foregroundStyle(TripletColor.grey600)
                    }

                    TripletCard {
                        SectionHeader(title: "앱 상태", subtitle: "현재 저장된 데이터")
                        HStack {
                            SummaryMetric(label: "현재 결제", value: "\(store.expenses.count)건")
                            SummaryMetric(label: "현재 경로", value: "\(store.locationSamples.count)점")
                            SummaryMetric(label: "여행", value: store.travelModeEnabled ? "진행 중" : "대기")
                        }
                    }
                }
                .padding(18)
            }
            .background(TripletColor.grey100)
            .navigationTitle("설정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") {
                        dismiss()
                    }
                    .font(.subheadline.weight(.bold))
                }
            }
            .sheet(isPresented: $demoFormPresented) {
                DemoNotificationFormView(notificationService: notificationService)
            }
        }
    }
}

struct TripManagementPage: View {
    let notificationService: DemoPaymentNotificationService

    var body: some View {
        NavigationStack {
            TripManagementContent()
                .navigationTitle("지난 여행")
                .tripletSettingsToolbar(notificationService: notificationService)
        }
    }
}

struct TripManagementContent: View {
    @EnvironmentObject private var store: TripletStore

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                SectionHeader(
                    title: "지난 여행",
                    subtitle: "저장된 여행 \(store.archivedTrips.count)개"
                )
                if store.archivedTrips.isEmpty {
                    TripletCard {
                        Text("아직 저장된 여행이 없습니다.")
                            .font(.headline.weight(.bold))
                        Text("여행모드를 켜고 소비를 기록한 뒤 여행 중지를 누르면 이곳에 자동 저장됩니다.")
                            .foregroundStyle(TripletColor.grey600)
                    }
                } else {
                    ForEach(store.archivedTrips) { trip in
                        NavigationLink {
                            TripDetailView(trip: trip)
                        } label: {
                            TripRecordRow(trip: trip)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(18)
        }
        .background(TripletColor.grey100)
    }
}

struct TripDetailView: View {
    var trip: TripRecord

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                TripletCard {
                    Text(trip.title)
                        .font(.title2.weight(.black))
                    Text(tripPeriod)
                        .font(.subheadline)
                        .foregroundStyle(TripletColor.grey600)
                    HStack {
                        SummaryMetric(label: "총 소비", value: TripletFormat.won(trip.totalAmountMinor))
                        SummaryMetric(label: "결제", value: "\(trip.expenses.count)건")
                        SummaryMetric(label: "경로", value: "\(trip.locationSamples.count)점")
                    }
                }

                TripletCard {
                    SectionHeader(title: "여행 지도", subtitle: "저장된 결제 위치")
                    TripMapView(expenses: trip.expenses, samples: trip.locationSamples, height: 420)
                }

                TripletCard {
                    SectionHeader(title: "여행 소비 내역", subtitle: "결제 \(trip.expenses.count)건")
                    ForEach(trip.expenses.sorted { $0.occurredAt > $1.occurredAt }) { expense in
                        ExpenseRow(expense: expense)
                    }
                }
            }
            .padding(18)
        }
        .background(TripletColor.grey100)
        .navigationTitle("여행 상세")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var tripPeriod: String {
        let started = TripletFormat.tripDate.string(from: trip.startedAt)
        guard let endedAt = trip.endedAt else { return started }
        return "\(started) - \(TripletFormat.tripDate.string(from: endedAt))"
    }
}
