import SwiftUI

struct EmptyExpenseCard: View {
    var body: some View {
        TripletCard {
            Text("아직 소비 내역이 없습니다.")
                .font(.headline.weight(.bold))
                .foregroundStyle(TripletColor.grey900)
        }
    }
}

struct ExpenseRow: View {
    var expense: Expense

    var body: some View {
        TripletCard {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(expense.merchantName)
                        .font(.headline.weight(.black))
                        .foregroundStyle(TripletColor.grey900)
                        .lineLimit(1)
                    Text(TripletFormat.displayDate.string(from: expense.occurredAt))
                        .font(.caption)
                        .foregroundStyle(TripletColor.grey600)
                }
                Spacer()
                Text(TripletFormat.won(expense.amountMinor))
                    .font(.headline.weight(.black))
                    .foregroundStyle(TripletColor.grey900)
            }

            HStack {
                InfoPill(
                    text: expense.category.displayName,
                    background: TripletColor.blue50,
                    foreground: TripletColor.blueDark
                )
                InfoPill(
                    text: expense.matchSource.displayName,
                    background: matchBackground,
                    foreground: matchForeground
                )
            }

            if let note = expense.note, !note.isEmpty {
                Text(note)
                    .font(.caption)
                    .foregroundStyle(TripletColor.grey600)
            }
        }
    }

    private var matchBackground: Color {
        switch expense.matchSource {
        case .demoCoordinate, .manual: return TripletColor.blue50
        case .locationSample: return Color.green.opacity(0.12)
        case .unmatched: return TripletColor.orange50
        }
    }

    private var matchForeground: Color {
        switch expense.matchSource {
        case .demoCoordinate, .manual: return TripletColor.blueDark
        case .locationSample: return TripletColor.green
        case .unmatched: return TripletColor.orange
        }
    }
}

struct SummaryCard: View {
    @EnvironmentObject private var store: TripletStore

    var body: some View {
        TripletCard {
            SectionHeader(title: "소비 요약", subtitle: "카테고리별 지출")
            HStack {
                SummaryMetric(label: "총 결제", value: TripletFormat.won(store.totalAmountMinor))
                SummaryMetric(label: "건수", value: "\(store.expenses.count)")
                SummaryMetric(label: "상위", value: store.topCategory?.displayName ?? "-")
            }

            if store.categoryBreakdown.isEmpty {
                Text("아직 집계할 소비 데이터가 없습니다.")
                    .foregroundStyle(TripletColor.grey600)
            } else {
                VStack(alignment: .leading, spacing: 12) {
                    RationalSpendingComment(
                        breakdown: store.categoryBreakdown,
                        total: max(store.totalAmountMinor, 1)
                    )
                    Text("분야별 지출 그래프")
                        .font(.subheadline.weight(.bold))
                    CategoryDonutChart(
                        breakdown: store.categoryBreakdown,
                        total: max(store.totalAmountMinor, 1)
                    )
                }
            }
        }
    }
}

struct RationalSpendingComment: View {
    var breakdown: [(ExpenseCategory, Int)]
    var total: Int

    var body: some View {
        let insight = makeInsight()
        ZStack(alignment: .bottomTrailing) {
            SpeechBubbleShape(cornerRadius: 20, tailWidth: 28, tailHeight: 18)
                .fill(insight.background)
                .overlay {
                    SpeechBubbleShape(cornerRadius: 20, tailWidth: 28, tailHeight: 18)
                        .stroke(insight.accent.opacity(0.34), lineWidth: 1.4)
                }

            HStack(alignment: .center, spacing: 14) {
                VStack(alignment: .leading, spacing: 7) {
                    Label("합리적인 소비를 위한 코멘트", systemImage: "sparkles")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(insight.accent)
                    Text(insight.title)
                        .font(.headline.weight(.black))
                        .foregroundStyle(TripletColor.grey900)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(insight.message)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(TripletColor.grey700)
                        .lineSpacing(3)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 6)

                SpendingMascot(kind: insight.mascot, accent: insight.accent)
                    .frame(width: 82, height: 82)
            }
            .padding(.top, 16)
            .padding(.horizontal, 16)
            .padding(.bottom, 28)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func makeInsight() -> (
        title: String,
        message: String,
        accent: Color,
        background: Color,
        mascot: SpendingMascot.Kind
    ) {
        guard let top = breakdown.first else {
            return (
                "아직 분석할 소비가 없어요.",
                "결제 데이터가 쌓이면 지갑 친구가 여행 소비 패턴을 알려드릴게요.",
                TripletColor.blue,
                TripletColor.blue50.opacity(0.72),
                .wallet
            )
        }

        let percentage = Int(((Double(top.1) / Double(total)) * 100).rounded())
        if percentage >= 45 {
            let mascot = mascot(for: top.0, isWarning: true)
            return (
                "오늘 \(top.0.displayName)가 전체 지출의 \(percentage)%예요.",
                warningMessage(for: top.0),
                TripletColor.orange,
                TripletColor.orange50.opacity(0.74),
                mascot
            )
        }

        if percentage <= 30 {
            return (
                "오늘 소비는 꽤 안정적이에요.",
                "가장 큰 지출도 \(percentage)%라서 한쪽으로 크게 쏠리지 않았어요. 지갑이 안심하고 있습니다.",
                TripletColor.green,
                Color.green.opacity(0.12),
                .happyWallet
            )
        }

        return (
            "\(top.0.displayName)가 살짝 앞서가고 있어요.",
            "현재 \(percentage)%로 가장 높지만 아직 위험하진 않아요. 다음 큰 결제 전에 한 번만 더 확인하면 충분합니다.",
            TripletColor.blue,
            TripletColor.blue50.opacity(0.72),
            .wallet
        )
    }

    private func mascot(for category: ExpenseCategory, isWarning: Bool) -> SpendingMascot.Kind {
        switch category {
        case .food, .cafe: return .noodle
        case .shopping: return .shoppingBag
        case .transport: return .mapPin
        case .culture: return .ticket
        case .lodging: return .suitcase
        case .etc: return isWarning ? .sadWallet : .wallet
        }
    }

    private func warningMessage(for category: ExpenseCategory) -> String {
        switch category {
        case .food:
            return "배는 부르고 예산은 비어갑니다. 다음 식사는 가볍게 가도 좋아요."
        case .cafe:
            return "커피 향은 좋지만 지갑은 잠깐 쉬고 싶대요. 다음 카페는 일정 끝에 한 번만!"
        case .shopping:
            return "가방보다 지갑이 더 가벼워지고 있어요. 기념품은 우선순위를 정해보세요."
        case .transport:
            return "이동비가 꽤 커졌어요. 가까운 코스는 도보 이동도 한 번 고려해보세요."
        case .culture:
            return "문화생활이 풍성한 대신 예산도 빠르게 쓰이고 있어요. 남은 관람 일정만 한번 체크!"
        case .lodging:
            return "숙박비 비중이 높아요. 남은 지출은 식비와 이동비 중심으로 조절하면 좋아요."
        case .etc:
            return "기타 지출이 많아졌어요. 어디에 썼는지 메모를 남기면 다음 여행 예산이 쉬워집니다."
        }
    }
}

struct SpeechBubbleShape: Shape {
    var cornerRadius: CGFloat
    var tailWidth: CGFloat
    var tailHeight: CGFloat

    func path(in rect: CGRect) -> Path {
        let bubbleRect = CGRect(
            x: rect.minX,
            y: rect.minY,
            width: rect.width,
            height: rect.height - tailHeight
        )
        var path = Path(roundedRect: bubbleRect, cornerRadius: cornerRadius)
        let tailStartX = rect.maxX - 78
        path.move(to: CGPoint(x: tailStartX, y: bubbleRect.maxY - 1))
        path.addLine(to: CGPoint(x: tailStartX + 18, y: rect.maxY))
        path.addLine(to: CGPoint(x: tailStartX + tailWidth, y: bubbleRect.maxY - 1))
        path.closeSubpath()
        return path
    }
}

struct SpendingMascot: View {
    enum Kind {
        case wallet
        case sadWallet
        case happyWallet
        case shoppingBag
        case noodle
        case mapPin
        case ticket
        case suitcase
    }

    var kind: Kind
    var accent: Color

    var body: some View {
        ZStack {
            switch kind {
            case .wallet:
                WalletMascot(color: accent, mood: .calm)
            case .sadWallet:
                WalletMascot(color: accent, mood: .sad)
            case .happyWallet:
                WalletMascot(color: accent, mood: .happy)
            case .shoppingBag:
                ShoppingBagMascot(color: Color(red: 1.0, green: 0.45, blue: 0.62))
            case .noodle:
                NoodleMascot(color: accent)
            case .mapPin:
                MapPinMascot(color: TripletColor.blue)
            case .ticket:
                TicketMascot(color: TripletColor.purple)
            case .suitcase:
                SuitcaseMascot(color: TripletColor.orange)
            }
        }
    }
}

private struct MascotFace: View {
    enum Mood {
        case calm
        case sad
        case happy
    }

    var mood: Mood

    var body: some View {
        VStack(spacing: 5) {
            HStack(spacing: 16) {
                Circle()
                    .fill(.white)
                    .frame(width: 7, height: 7)
                Circle()
                    .fill(.white)
                    .frame(width: 7, height: 7)
            }
            mouth
                .stroke(.white, lineWidth: 3)
                .frame(width: 18, height: 10)
        }
    }

    private var mouth: some Shape {
        switch mood {
        case .calm:
            return AnyShape(Capsule())
        case .sad:
            return AnyShape(SadMouthShape())
        case .happy:
            return AnyShape(HappyMouthShape())
        }
    }
}

private struct WalletMascot: View {
    enum Mood {
        case calm
        case sad
        case happy
    }

    var color: Color
    var mood: Mood

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(color)
                .rotationEffect(.degrees(-5))
                .shadow(color: color.opacity(0.22), radius: 8, y: 5)
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(.white.opacity(0.22))
                .frame(width: 40, height: 18)
                .offset(x: 9, y: -18)
            Circle()
                .fill(.white.opacity(0.85))
                .frame(width: 8, height: 8)
                .offset(x: 20, y: -18)
            MascotFace(mood: faceMood)
                .offset(y: 6)
            if mood == .sad {
                Text("💧")
                    .font(.title3)
                    .offset(x: 28, y: 14)
            }
        }
        .padding(7)
    }

    private var faceMood: MascotFace.Mood {
        switch mood {
        case .calm: return .calm
        case .sad: return .sad
        case .happy: return .happy
        }
    }
}

private struct ShoppingBagMascot: View {
    var color: Color

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 13, style: .continuous)
                .fill(color)
                .frame(width: 58, height: 58)
                .offset(y: 8)
                .shadow(color: color.opacity(0.22), radius: 8, y: 5)
            BagHandleShape()
                .stroke(color.opacity(0.72), lineWidth: 5)
                .frame(width: 32, height: 22)
                .offset(y: -20)
            MascotFace(mood: .sad)
                .offset(y: 13)
            Text("✨")
                .font(.caption)
                .offset(x: 28, y: -18)
        }
    }
}

private struct NoodleMascot: View {
    var color: Color

    var body: some View {
        ZStack {
            Ellipse()
                .fill(Color.white)
                .frame(width: 66, height: 48)
                .overlay {
                    Ellipse().stroke(color.opacity(0.55), lineWidth: 4)
                }
                .offset(y: 8)
                .shadow(color: color.opacity(0.18), radius: 8, y: 5)
            RoundedRectangle(cornerRadius: 3)
                .fill(color)
                .frame(width: 5, height: 54)
                .rotationEffect(.degrees(28))
                .offset(x: 20, y: -18)
            RoundedRectangle(cornerRadius: 3)
                .fill(color.opacity(0.75))
                .frame(width: 5, height: 54)
                .rotationEffect(.degrees(28))
                .offset(x: 30, y: -18)
            MascotFace(mood: .sad)
                .foregroundStyle(color)
                .offset(y: 11)
            Text("💧")
                .font(.caption)
                .offset(x: 31, y: 19)
        }
    }
}

private struct MapPinMascot: View {
    var color: Color

    var body: some View {
        ZStack {
            Image(systemName: "mappin.circle.fill")
                .resizable()
                .scaledToFit()
                .foregroundStyle(color)
                .shadow(color: color.opacity(0.2), radius: 8, y: 5)
            MascotFace(mood: .sad)
                .scaleEffect(0.75)
                .offset(y: -2)
        }
        .padding(6)
    }
}

private struct TicketMascot: View {
    var color: Color

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(color)
                .frame(width: 66, height: 46)
                .rotationEffect(.degrees(-8))
                .shadow(color: color.opacity(0.2), radius: 8, y: 5)
            MascotFace(mood: .calm)
                .offset(y: 2)
        }
    }
}

private struct SuitcaseMascot: View {
    var color: Color

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(color)
                .frame(width: 62, height: 54)
                .offset(y: 7)
                .shadow(color: color.opacity(0.2), radius: 8, y: 5)
            RoundedRectangle(cornerRadius: 5)
                .stroke(color.opacity(0.7), lineWidth: 5)
                .frame(width: 28, height: 20)
                .offset(y: -20)
            MascotFace(mood: .sad)
                .offset(y: 10)
        }
    }
}

private struct HappyMouthShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.addArc(
            center: CGPoint(x: rect.midX, y: rect.minY),
            radius: rect.width / 2,
            startAngle: .degrees(20),
            endAngle: .degrees(160),
            clockwise: false
        )
        return path
    }
}

private struct SadMouthShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.addArc(
            center: CGPoint(x: rect.midX, y: rect.maxY),
            radius: rect.width / 2,
            startAngle: .degrees(200),
            endAngle: .degrees(340),
            clockwise: false
        )
        return path
    }
}

private struct BagHandleShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.addArc(
            center: CGPoint(x: rect.midX, y: rect.maxY),
            radius: rect.width / 2,
            startAngle: .degrees(200),
            endAngle: .degrees(340),
            clockwise: false
        )
        return path
    }
}

private struct AnyShape: Shape {
    private let makePath: (CGRect) -> Path

    init<S: Shape>(_ shape: S) {
        makePath = { rect in
            shape.path(in: rect)
        }
    }

    func path(in rect: CGRect) -> Path {
        makePath(rect)
    }
}

struct SummaryMetric: View {
    var label: String
    var value: String

    var body: some View {
        VStack(spacing: 5) {
            Text(value)
                .font(.subheadline.weight(.black))
                .foregroundStyle(TripletColor.grey900)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(label)
                .font(.caption)
                .foregroundStyle(TripletColor.grey500)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(TripletColor.grey50)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct CategoryDonutChart: View {
    var breakdown: [(ExpenseCategory, Int)]
    var total: Int

    var body: some View {
        VStack(spacing: 18) {
            ZStack {
                Circle()
                    .stroke(TripletColor.grey100, lineWidth: 28)

                ForEach(Array(segments.enumerated()), id: \.offset) { _, segment in
                    DonutSegmentShape(
                        startAngle: segment.start,
                        endAngle: segment.end
                    )
                    .stroke(
                        segment.category.tint,
                        style: StrokeStyle(lineWidth: 28, lineCap: .butt)
                    )
                }

                VStack(spacing: 3) {
                    Text("총 지출")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(TripletColor.grey500)
                    Text(TripletFormat.won(total))
                        .font(.headline.weight(.black))
                        .foregroundStyle(TripletColor.grey900)
                        .lineLimit(1)
                        .minimumScaleFactor(0.65)
                }
            }
            .frame(width: 190, height: 190)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)

            VStack(spacing: 10) {
                ForEach(breakdown, id: \.0) { category, amount in
                    CategoryLegendRow(
                        category: category,
                        amount: amount,
                        total: total
                    )
                }
            }
        }
    }

    private var segments: [(category: ExpenseCategory, start: Angle, end: Angle)] {
        var start = -90.0
        return breakdown.map { category, amount in
            let degrees = max((Double(amount) / Double(total)) * 360.0, 2.0)
            let segment = (
                category: category,
                start: Angle(degrees: start),
                end: Angle(degrees: start + degrees)
            )
            start += degrees
            return segment
        }
    }
}

struct DonutSegmentShape: Shape {
    var startAngle: Angle
    var endAngle: Angle

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2
        path.addArc(
            center: center,
            radius: radius,
            startAngle: startAngle,
            endAngle: endAngle,
            clockwise: false
        )
        return path
    }
}

struct CategoryLegendRow: View {
    var category: ExpenseCategory
    var amount: Int
    var total: Int

    var body: some View {
        let percent = Int(((Double(amount) / Double(total)) * 100).rounded())
        HStack(spacing: 10) {
            Circle()
                .fill(category.tint)
                .frame(width: 10, height: 10)
            Text(category.displayName)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(TripletColor.grey900)
            Spacer()
            Text("\(TripletFormat.won(amount)) · \(percent)%")
                .font(.caption.weight(.semibold))
                .foregroundStyle(TripletColor.grey600)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(TripletColor.grey50)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct TripRecordRow: View {
    var trip: TripRecord

    var body: some View {
        TripletCard {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(trip.title)
                        .font(.headline.weight(.black))
                        .foregroundStyle(TripletColor.grey900)
                        .lineLimit(1)
                    Text(TripletFormat.tripDate.string(from: trip.startedAt))
                        .font(.caption)
                        .foregroundStyle(TripletColor.grey600)
                }
                Spacer()
                Text(TripletFormat.won(trip.totalAmountMinor))
                    .font(.headline.weight(.black))
                    .foregroundStyle(TripletColor.grey900)
            }
            HStack {
                InfoPill(text: "결제 \(trip.expenses.count)건")
                InfoPill(text: "경로 \(trip.locationSamples.count)점", background: TripletColor.grey100, foreground: TripletColor.grey600)
                InfoPill(text: trip.topCategory?.displayName ?? "분석 대기", background: TripletColor.orange50, foreground: TripletColor.orange)
            }
        }
    }
}
