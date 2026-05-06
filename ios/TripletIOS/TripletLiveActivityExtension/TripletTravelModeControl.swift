import AppIntents
import SwiftUI
import WidgetKit

@available(iOS 18.0, *)
struct TripletTravelModeControl: ControlWidget {
    static let kind = "com.triplet.ios.travel-mode-control"

    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(
            kind: Self.kind,
            provider: TripletTravelModeControlProvider()
        ) { isOn in
            ControlWidgetToggle(
                "여행모드",
                isOn: isOn,
                action: SetTripletTravelModeIntent()
            ) { value in
                Label(
                    value ? "켬" : "끔",
                    systemImage: value ? "location.fill" : "location"
                )
            }
            .tint(.blue)
        }
        .displayName("트립렛 여행모드")
        .description("제어센터에서 트립렛 여행모드를 켜고 끕니다.")
    }
}

@available(iOS 18.0, *)
struct TripletTravelModeControlProvider: ControlValueProvider {
    var previewValue: Bool {
        false
    }

    func currentValue() async throws -> Bool {
        TripletSharedTravelState.isTravelModeEnabled
    }
}

@available(iOS 18.0, *)
struct SetTripletTravelModeIntent: SetValueIntent {
    static var title: LocalizedStringResource = "트립렛 여행모드 전환"

    @Parameter(title: "여행모드")
    var value: Bool

    init() {
        value = false
    }

    func perform() async throws -> some IntentResult {
        TripletSharedTravelState.setTravelModeEnabled(value)
        ControlCenter.shared.reloadControls(ofKind: "com.triplet.ios.travel-mode-control")
        return .result()
    }
}
