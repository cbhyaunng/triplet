import ActivityKit
import SwiftUI
import WidgetKit

@main
struct TripletLiveActivityBundle: WidgetBundle {
    var body: some Widget {
        TripletTravelLiveActivity()
        if #available(iOS 18.0, *) {
            TripletTravelModeControl()
        }
    }
}

struct TripletTravelLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TripletTravelActivityAttributes.self) { context in
            LockScreenLiveActivityView(context: context)
                .activityBackgroundTint(Color(red: 0.10, green: 0.12, blue: 0.16).opacity(0.92))
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label("여행 중", systemImage: "location.north.line.fill")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(won(context.state.totalAmountMinor))
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(context.state.latestMerchantName ?? "현재 위치 추적 중")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                        HStack(spacing: 8) {
                            MiniMetric(text: "결제 \(context.state.expenseCount)건")
                            MiniMetric(text: "경로 \(context.state.routePointCount)점")
                        }
                    }
                }
            } compactLeading: {
                Image(systemName: "location.north.line.fill")
                    .foregroundStyle(.blue)
            } compactTrailing: {
                Text("\(context.state.expenseCount)")
                    .font(.caption2.weight(.black))
                    .foregroundStyle(.white)
            } minimal: {
                Image(systemName: "location.north.fill")
                    .foregroundStyle(.blue)
            }
        }
    }
}

private struct LockScreenLiveActivityView: View {
    let context: ActivityViewContext<TripletTravelActivityAttributes>

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color.white)
                    Image(systemName: "map.fill")
                        .font(.title3.weight(.black))
                        .foregroundStyle(Color(red: 0.20, green: 0.51, blue: 0.97))
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 4) {
                    Text("트립렛 여행모드")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                    Text(context.state.isTracking ? "현재 위치 추적 중" : "여행 기록 저장 중")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.72))
                }

                Spacer()

                Text(context.state.isTracking ? "ON" : "완료")
                    .font(.caption.weight(.black))
                    .foregroundStyle(context.state.isTracking ? Color(red: 0.26, green: 0.86, blue: 0.39) : .white.opacity(0.8))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(.white.opacity(0.14))
                    .clipShape(Capsule())
            }

            Divider()
                .overlay(.white.opacity(0.12))

            HStack(spacing: 10) {
                LiveMetric(title: "결제", value: "\(context.state.expenseCount)건")
                LiveMetric(title: "소비", value: won(context.state.totalAmountMinor))
                LiveMetric(title: "경로", value: "\(context.state.routePointCount)점")
            }

            if let merchant = context.state.latestMerchantName {
                HStack(spacing: 7) {
                    Image(systemName: "creditcard.fill")
                    Text(merchant)
                        .lineLimit(1)
                    if let amount = context.state.latestAmountMinor {
                        Text(won(amount))
                    }
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.76))
            }
        }
        .padding(16)
    }
}

private struct LiveMetric: View {
    var title: String
    var value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.white.opacity(0.62))
            Text(value)
                .font(.subheadline.weight(.black))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(.white.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct MiniMetric: View {
    var text: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.bold))
            .foregroundStyle(.white.opacity(0.8))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(.white.opacity(0.12))
            .clipShape(Capsule())
    }
}

private func won(_ amount: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.locale = Locale(identifier: "ko_KR")
    return "\(formatter.string(from: NSNumber(value: amount)) ?? "\(amount)")원"
}
