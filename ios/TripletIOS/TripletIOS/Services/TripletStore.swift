import Foundation

struct TripletSnapshot: Codable {
    var travelModeEnabled: Bool
    var activeTripStartedAt: Date?
    var locationSamples: [LocationSample]
    var expenses: [Expense]
    var archivedTrips: [TripRecord]
    var logs: [String]
}

@MainActor
final class TripletStore: ObservableObject {
    @Published var selectedTab: TripletTab = .home
    @Published var travelModeEnabled = false
    @Published var activeTripStartedAt: Date?
    @Published var locationSamples: [LocationSample] = []
    @Published var expenses: [Expense] = []
    @Published var archivedTrips: [TripRecord] = []
    @Published var logs: [String] = []

    private var notificationObserver: NSObjectProtocol?
    private let liveActivityService = TripletLiveActivityService()

    init() {
        restore()
        notificationObserver = NotificationCenter.default.addObserver(
            forName: .tripletDemoPaymentReceived,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let userInfo = notification.userInfo else { return }
            self?.handleNotificationUserInfo(userInfo)
        }
    }

    deinit {
        if let notificationObserver {
            NotificationCenter.default.removeObserver(notificationObserver)
        }
    }

    var totalAmountMinor: Int {
        expenses.reduce(0) { $0 + $1.amountMinor }
    }

    var topCategory: ExpenseCategory? {
        categoryBreakdown.first?.0
    }

    var categoryBreakdown: [(ExpenseCategory, Int)] {
        Dictionary(grouping: expenses, by: \.category)
            .map { category, items in
                (category, items.reduce(0) { $0 + $1.amountMinor })
            }
            .sorted { $0.1 > $1.1 }
    }

    var sortedExpenses: [Expense] {
        expenses.sorted { $0.occurredAt > $1.occurredAt }
    }

    func startTrip(startedAt: Date = Date(), updateSharedState: Bool = true) {
        locationSamples.removeAll()
        expenses.removeAll()
        travelModeEnabled = true
        activeTripStartedAt = startedAt
        if updateSharedState {
            TripletSharedTravelState.setTravelModeEnabled(true, startedAt: startedAt)
        }
        appendLog("여행모드 시작")
        persist()
        startLiveActivity()
    }

    @discardableResult
    func stopTrip(updateSharedState: Bool = true) -> TripRecord? {
        travelModeEnabled = false
        defer { persist() }

        guard !locationSamples.isEmpty || !expenses.isEmpty else {
            let finalState = makeLiveActivityState(isTracking: false)
            activeTripStartedAt = nil
            if updateSharedState {
                TripletSharedTravelState.setTravelModeEnabled(false)
            }
            appendLog("저장할 데이터 없이 여행모드 종료")
            liveActivityService.end(state: finalState)
            return nil
        }

        let startedAt = activeTripStartedAt
            ?? expenses.map(\.occurredAt).min()
            ?? locationSamples.map(\.capturedAt).min()
            ?? Date()
        let title = makeTripTitle(startedAt: startedAt, expenses: expenses)
        let trip = TripRecord(
            id: UUID(),
            title: title,
            startedAt: startedAt,
            endedAt: Date(),
            locationSamples: locationSamples.sorted { $0.capturedAt < $1.capturedAt },
            expenses: expenses.sorted { $0.occurredAt < $1.occurredAt }
        )

        archivedTrips.insert(trip, at: 0)
        if archivedTrips.count > 30 {
            archivedTrips.removeLast(archivedTrips.count - 30)
        }
        let finalState = makeLiveActivityState(isTracking: false)
        locationSamples.removeAll()
        expenses.removeAll()
        activeTripStartedAt = nil
        if updateSharedState {
            TripletSharedTravelState.setTravelModeEnabled(false)
        }
        selectedTab = .trips
        appendLog("여행 기록 저장 완료: \(trip.title)")
        liveActivityService.end(state: finalState)
        return trip
    }

    func addLocationSample(_ sample: LocationSample) {
        guard travelModeEnabled else { return }
        if let latest = locationSamples.last {
            let veryClose = abs(latest.latitude - sample.latitude) < 0.00001 &&
                abs(latest.longitude - sample.longitude) < 0.00001
            if veryClose { return }
        }
        locationSamples.append(sample)
        if locationSamples.count > 200 {
            locationSamples.removeFirst(locationSamples.count - 200)
        }
        persist()
        updateLiveActivity()
    }

    func addManualExpense(
        merchantName: String,
        amountMinor: Int,
        occurredAt: Date,
        category: ExpenseCategory,
        note: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        let expense = Expense(
            id: UUID(),
            txId: "manual-\(UUID().uuidString)",
            merchantName: merchantName,
            amountMinor: amountMinor,
            currencyCode: "KRW",
            occurredAt: occurredAt,
            category: category,
            note: note,
            latitude: latitude,
            longitude: longitude,
            matchSource: latitude == nil || longitude == nil ? .unmatched : .manual
        )
        addOrReplaceExpense(expense)
        appendLog("수동 소비 입력: \(merchantName)")
    }

    func addExpense(from payload: PaymentNotificationPayload) {
        let nearest = nearestLocation(to: payload.occurredAt)
        let hasDemoCoordinate = payload.latitude != nil && payload.longitude != nil
        let expense = Expense(
            id: UUID(),
            txId: payload.txId,
            merchantName: payload.merchantName,
            amountMinor: payload.amountMinor,
            currencyCode: payload.currencyCode,
            occurredAt: payload.occurredAt,
            category: payload.category,
            note: payload.note,
            latitude: payload.latitude ?? nearest?.latitude,
            longitude: payload.longitude ?? nearest?.longitude,
            matchSource: hasDemoCoordinate ? .demoCoordinate : (nearest == nil ? .unmatched : .locationSample)
        )
        addOrReplaceExpense(expense)
        appendLog(hasDemoCoordinate ? "시연 좌표 사용: \(payload.merchantName)" : "알림 위치 매칭: \(payload.merchantName)")
    }

    func clearCurrentData() {
        locationSamples.removeAll()
        expenses.removeAll()
        appendLog("현재 여행 데이터 초기화")
        persist()
        updateLiveActivity()
    }

    func insertDemoData() {
        let now = Date()
        let startedAt = Calendar.current.date(byAdding: .hour, value: -4, to: now) ?? now
        let routeCoordinates = [
            (37.5790, 126.9770),
            (37.5796, 126.9864),
            (37.5774, 126.9827),
            (37.5795, 126.9800),
            (37.5701, 126.9997),
            (37.5663, 127.0095)
        ]

        travelModeEnabled = true
        activeTripStartedAt = startedAt
        TripletSharedTravelState.setTravelModeEnabled(true, startedAt: startedAt)

        locationSamples.removeAll { $0.source == "settings-demo" }
        let demoSamples = routeCoordinates.enumerated().map { index, coordinate in
            LocationSample(
                id: UUID(),
                latitude: coordinate.0,
                longitude: coordinate.1,
                horizontalAccuracy: 8,
                capturedAt: startedAt.addingTimeInterval(TimeInterval(index * 45 * 60)),
                source: "settings-demo"
            )
        }
        locationSamples.append(contentsOf: demoSamples)
        locationSamples.sort { $0.capturedAt < $1.capturedAt }

        expenses.removeAll { $0.txId.hasPrefix("settings-demo-") }
        let demoExpenses = DemoTemplates.all.enumerated().map { index, template in
            Expense(
                id: UUID(),
                txId: "settings-demo-\(index)",
                merchantName: template.merchantName,
                amountMinor: template.amountMinor,
                currencyCode: "KRW",
                occurredAt: startedAt.addingTimeInterval(TimeInterval((index + 1) * 45 * 60)),
                category: template.category,
                note: template.note,
                latitude: template.latitude,
                longitude: template.longitude,
                matchSource: .demoCoordinate
            )
        }
        expenses.append(contentsOf: demoExpenses)
        expenses.sort { $0.occurredAt < $1.occurredAt }
        selectedTab = .home
        appendLog("설정에서 데모 데이터 입력")
        persist()
        liveActivityService.start(
            startedAt: startedAt,
            state: makeLiveActivityState()
        )
    }

    func deleteArchivedTrip(_ trip: TripRecord) {
        archivedTrips.removeAll { $0.id == trip.id }
        appendLog("지난 여행 삭제: \(trip.title)")
        persist()
    }

    @discardableResult
    func applySharedTravelModeIfNeeded() -> Bool {
        let sharedIsEnabled = TripletSharedTravelState.isTravelModeEnabled
        guard sharedIsEnabled != travelModeEnabled else { return false }

        if sharedIsEnabled {
            startTrip(
                startedAt: TripletSharedTravelState.startedAt ?? Date(),
                updateSharedState: false
            )
            appendLog("제어센터에서 여행모드 시작")
        } else {
            _ = stopTrip(updateSharedState: false)
            appendLog("제어센터에서 여행모드 종료")
        }
        return true
    }

    private func handleNotificationUserInfo(_ userInfo: [AnyHashable: Any]) {
        guard travelModeEnabled else {
            appendLog("여행모드가 꺼져 있어 시연 알림 무시")
            return
        }
        do {
            let payload = try PaymentNotificationParser.parse(userInfo: userInfo)
            addExpense(from: payload)
        } catch {
            appendLog("알림 파싱 실패: \(error.localizedDescription)")
        }
    }

    private func addOrReplaceExpense(_ expense: Expense) {
        if let index = expenses.firstIndex(where: { $0.txId == expense.txId }) {
            expenses[index] = expense
        } else {
            expenses.append(expense)
        }
        expenses.sort { $0.occurredAt < $1.occurredAt }
        persist()
        updateLiveActivity()
    }

    private func startLiveActivity() {
        guard let activeTripStartedAt else { return }
        liveActivityService.start(
            startedAt: activeTripStartedAt,
            state: makeLiveActivityState()
        )
    }

    private func updateLiveActivity() {
        guard travelModeEnabled else { return }
        liveActivityService.update(state: makeLiveActivityState())
    }

    private func makeLiveActivityState(isTracking: Bool? = nil) -> TripletTravelActivityAttributes.ContentState {
        let latestExpense = expenses.max { $0.occurredAt < $1.occurredAt }
        return TripletTravelActivityAttributes.ContentState(
            isTracking: isTracking ?? travelModeEnabled,
            expenseCount: expenses.count,
            routePointCount: locationSamples.count,
            totalAmountMinor: totalAmountMinor,
            latestMerchantName: latestExpense?.merchantName,
            latestAmountMinor: latestExpense?.amountMinor,
            lastUpdatedAt: Date()
        )
    }

    private func nearestLocation(to date: Date) -> LocationSample? {
        locationSamples.min {
            abs($0.capturedAt.timeIntervalSince(date)) < abs($1.capturedAt.timeIntervalSince(date))
        }
    }

    private func makeTripTitle(startedAt: Date, expenses: [Expense]) -> String {
        let date = TripletFormat.tripDate.string(from: startedAt).prefix(10)
        if let firstMerchant = expenses.sorted(by: { $0.occurredAt < $1.occurredAt }).first?.merchantName {
            return "\(date) \(firstMerchant)"
        }
        return "\(date) 여행"
    }

    private func appendLog(_ message: String) {
        let timestamp = TripletFormat.displayDate.string(from: Date())
        logs.insert("\(timestamp) \(message)", at: 0)
        if logs.count > 50 {
            logs.removeLast(logs.count - 50)
        }
    }

    private func restore() {
        guard let data = try? Data(contentsOf: snapshotURL),
              let snapshot = try? JSONDecoder.triplet.decode(TripletSnapshot.self, from: data) else {
            return
        }
        travelModeEnabled = false
        activeTripStartedAt = snapshot.activeTripStartedAt
        locationSamples = snapshot.locationSamples
        expenses = snapshot.expenses
        archivedTrips = snapshot.archivedTrips
        logs = snapshot.logs
    }

    private func persist() {
        let snapshot = TripletSnapshot(
            travelModeEnabled: travelModeEnabled,
            activeTripStartedAt: activeTripStartedAt,
            locationSamples: locationSamples,
            expenses: expenses,
            archivedTrips: archivedTrips,
            logs: logs
        )
        guard let data = try? JSONEncoder.triplet.encode(snapshot) else { return }
        try? data.write(to: snapshotURL, options: [.atomic])
    }

    private var snapshotURL: URL {
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return directory.appendingPathComponent("triplet-ios-state.json")
    }
}

extension JSONEncoder {
    static var triplet: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return encoder
    }
}

extension JSONDecoder {
    static var triplet: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}
