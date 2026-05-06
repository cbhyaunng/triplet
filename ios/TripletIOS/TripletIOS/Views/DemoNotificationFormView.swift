import SwiftUI

struct DemoNotificationFormView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: TripletStore

    let notificationService: DemoPaymentNotificationService

    @State private var merchantName = DemoTemplates.all[0].merchantName
    @State private var amountText = "\(DemoTemplates.all[0].amountMinor)"
    @State private var currencyCode = "KRW"
    @State private var category: ExpenseCategory = DemoTemplates.all[0].category
    @State private var note = DemoTemplates.all[0].note
    @State private var latitudeText = "\(DemoTemplates.all[0].latitude)"
    @State private var longitudeText = "\(DemoTemplates.all[0].longitude)"
    @State private var delayText = "0"
    @State private var statusMessage = "Triplet 앱 내부 로컬 알림으로 결제 시연을 진행합니다."

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    TripletCard {
                        Text("빠른 템플릿")
                            .font(.headline.weight(.bold))
                        ForEach(DemoTemplates.all) { template in
                            Button {
                                apply(template)
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(template.merchantName)
                                            .font(.subheadline.weight(.bold))
                                        Text("\(TripletFormat.won(template.amountMinor)) · \(template.category.displayName)")
                                            .font(.caption)
                                            .foregroundStyle(TripletColor.grey600)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .foregroundStyle(TripletColor.grey500)
                                }
                            }
                            .buttonStyle(.plain)
                            if template.id != DemoTemplates.all.last?.id {
                                Divider()
                            }
                        }
                    }

                    TripletCard {
                        Text("결제 알림 입력")
                            .font(.headline.weight(.bold))
                        TextField("가맹점명", text: $merchantName)
                            .textFieldStyle(.roundedBorder)
                        TextField("금액", text: $amountText)
                            .keyboardType(.numberPad)
                            .textFieldStyle(.roundedBorder)
                        TextField("통화 코드", text: $currencyCode)
                            .textInputAutocapitalization(.characters)
                            .textFieldStyle(.roundedBorder)
                        Picker("카테고리", selection: $category) {
                            ForEach(ExpenseCategory.allCases) { item in
                                Text(item.displayName).tag(item)
                            }
                        }
                        .pickerStyle(.segmented)
                        TextField("메모", text: $note)
                            .textFieldStyle(.roundedBorder)

                        HStack {
                            TextField("위도", text: $latitudeText)
                                .keyboardType(.decimalPad)
                                .textFieldStyle(.roundedBorder)
                            TextField("경도", text: $longitudeText)
                                .keyboardType(.decimalPad)
                                .textFieldStyle(.roundedBorder)
                        }
                        Text("좌표를 입력하면 GPS 샘플 없이도 지도에 바로 핀이 표시됩니다.")
                            .font(.caption)
                            .foregroundStyle(TripletColor.grey600)

                        TextField("몇 초 뒤 발송", text: $delayText)
                            .keyboardType(.numberPad)
                            .textFieldStyle(.roundedBorder)

                        if !store.travelModeEnabled {
                            Text("여행모드가 꺼져 있으면 알림이 와도 소비내역에는 추가되지 않습니다.")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(TripletColor.orange)
                        }

                        Button {
                            sendNotification()
                        } label: {
                            Label("알림 보내기", systemImage: "bell.badge.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(TripletColor.blue)
                        .controlSize(.large)

                        Text(statusMessage)
                            .font(.caption)
                            .foregroundStyle(TripletColor.grey600)
                    }
                }
                .padding(18)
            }
            .background(TripletColor.grey100)
            .navigationTitle("시연 알림")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") {
                        dismiss()
                    }
                }
            }
        }
    }

    private func apply(_ template: DemoPaymentTemplate) {
        merchantName = template.merchantName
        amountText = "\(template.amountMinor)"
        category = template.category
        note = template.note
        latitudeText = "\(template.latitude)"
        longitudeText = "\(template.longitude)"
    }

    private func sendNotification() {
        guard !merchantName.trimmingCharacters(in: .whitespaces).isEmpty else {
            statusMessage = "가맹점명을 입력해 주세요."
            return
        }
        guard let amount = Int(amountText), amount > 0 else {
            statusMessage = "금액을 숫자로 입력해 주세요."
            return
        }
        guard let latitude = Double(latitudeText), (-90...90).contains(latitude) else {
            statusMessage = "위도를 -90~90 사이 숫자로 입력해 주세요."
            return
        }
        guard let longitude = Double(longitudeText), (-180...180).contains(longitude) else {
            statusMessage = "경도를 -180~180 사이 숫자로 입력해 주세요."
            return
        }
        guard let delay = Int(delayText), (0...300).contains(delay) else {
            statusMessage = "발송 지연 시간은 0~300초 사이로 입력해 주세요."
            return
        }

        notificationService.requestAuthorization { granted in
            guard granted else {
                statusMessage = "알림 권한이 허용되지 않았습니다."
                return
            }
            let occurredAt = Date().addingTimeInterval(TimeInterval(delay))
            let payload = PaymentNotificationPayload(
                txId: "ios-\(Int(Date().timeIntervalSince1970))-\(UUID().uuidString.prefix(6))",
                merchantName: merchantName.trimmingCharacters(in: .whitespaces),
                amountMinor: amount,
                currencyCode: currencyCode.isEmpty ? "KRW" : currencyCode.uppercased(),
                occurredAt: occurredAt,
                category: category,
                note: note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
                latitude: latitude,
                longitude: longitude
            )
            notificationService.send(payload: payload, delaySeconds: delay) { result in
                switch result {
                case .success:
                    statusMessage = delay == 0 ? "알림 발송 완료" : "\(delay)초 뒤 알림 발송 예정"
                case .failure(let error):
                    statusMessage = "알림 발송 실패: \(error.localizedDescription)"
                }
            }
        }
    }
}

struct ManualExpenseEntryView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: TripletStore

    @State private var merchantName = ""
    @State private var amountText = ""
    @State private var dateText = TripletFormat.inputDate.string(from: Date())
    @State private var category: ExpenseCategory = .food
    @State private var note = ""
    @State private var latitudeText = ""
    @State private var longitudeText = ""
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                TripletCard {
                    Text("소비 수동 입력")
                        .font(.headline.weight(.bold))
                    TextField("장소", text: $merchantName)
                        .textFieldStyle(.roundedBorder)
                    TextField("금액", text: $amountText)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                    TextField("날짜시간 yyyy-MM-dd HH:mm", text: $dateText)
                        .textFieldStyle(.roundedBorder)
                    Picker("카테고리", selection: $category) {
                        ForEach(ExpenseCategory.allCases) { item in
                            Text(item.displayName).tag(item)
                        }
                    }
                    .pickerStyle(.segmented)
                    TextField("메모", text: $note)
                        .textFieldStyle(.roundedBorder)
                    HStack {
                        TextField("위도 선택 입력", text: $latitudeText)
                            .keyboardType(.decimalPad)
                            .textFieldStyle(.roundedBorder)
                        TextField("경도 선택 입력", text: $longitudeText)
                            .keyboardType(.decimalPad)
                            .textFieldStyle(.roundedBorder)
                    }
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(TripletColor.red)
                    }
                    Button("소비 추가") {
                        save()
                    }
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.borderedProminent)
                    .tint(TripletColor.blue)
                }
                .padding(18)
            }
            .background(TripletColor.grey100)
            .navigationTitle("수동 입력")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
        }
    }

    private func save() {
        guard !merchantName.trimmingCharacters(in: .whitespaces).isEmpty else {
            errorMessage = "장소를 입력해 주세요."
            return
        }
        guard let amount = Int(amountText), amount > 0 else {
            errorMessage = "금액을 숫자로 입력해 주세요."
            return
        }
        guard let date = TripletFormat.inputDate.date(from: dateText) else {
            errorMessage = "날짜시간 형식을 확인해 주세요."
            return
        }
        let latitude = latitudeText.isEmpty ? nil : Double(latitudeText)
        let longitude = longitudeText.isEmpty ? nil : Double(longitudeText)
        store.addManualExpense(
            merchantName: merchantName.trimmingCharacters(in: .whitespaces),
            amountMinor: amount,
            occurredAt: date,
            category: category,
            note: note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
            latitude: latitude,
            longitude: longitude
        )
        dismiss()
    }
}
