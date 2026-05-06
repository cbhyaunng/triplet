import SwiftUI

enum TripletColor {
    static let grey50 = Color(red: 0.976, green: 0.980, blue: 0.984)
    static let grey100 = Color(red: 0.949, green: 0.957, blue: 0.965)
    static let grey200 = Color(red: 0.898, green: 0.910, blue: 0.922)
    static let grey500 = Color(red: 0.545, green: 0.584, blue: 0.631)
    static let grey600 = Color(red: 0.420, green: 0.463, blue: 0.518)
    static let grey700 = Color(red: 0.306, green: 0.349, blue: 0.408)
    static let grey800 = Color(red: 0.200, green: 0.239, blue: 0.294)
    static let grey900 = Color(red: 0.098, green: 0.122, blue: 0.157)
    static let blue50 = Color(red: 0.910, green: 0.953, blue: 1.000)
    static let blue = Color(red: 0.192, green: 0.510, blue: 0.965)
    static let blueDark = Color(red: 0.133, green: 0.447, blue: 0.922)
    static let orange50 = Color(red: 1.000, green: 0.953, blue: 0.878)
    static let orange = Color(red: 0.961, green: 0.471, blue: 0.000)
    static let green = Color(red: 0.008, green: 0.576, blue: 0.349)
    static let teal = Color(red: 0.000, green: 0.659, blue: 0.537)
    static let purple = Color(red: 0.486, green: 0.361, blue: 1.000)
    static let red = Color(red: 0.894, green: 0.161, blue: 0.224)
}

struct TripletCard<Content: View>: View {
    var content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            content
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct InfoPill: View {
    var text: String
    var background: Color = TripletColor.blue50
    var foreground: Color = TripletColor.blueDark

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundStyle(foreground)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(background)
            .clipShape(Capsule())
    }
}

struct SectionHeader: View {
    var title: String
    var subtitle: String?
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(TripletColor.grey900)
                if let subtitle {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(TripletColor.grey600)
                }
            }
            Spacer()
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.subheadline.weight(.bold))
                    .buttonStyle(.borderedProminent)
                    .tint(TripletColor.blue50)
                    .foregroundStyle(TripletColor.blueDark)
            }
        }
    }
}

enum TripletFormat {
    static let currency: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = Locale(identifier: "ko_KR")
        return formatter
    }()

    static let inputDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        return formatter
    }()

    static let displayDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "MM-dd HH:mm"
        return formatter
    }()

    static let tripDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyy.MM.dd HH:mm"
        return formatter
    }()

    static let iso8601 = ISO8601DateFormatter()

    static func won(_ amount: Int) -> String {
        "\(currency.string(from: NSNumber(value: amount)) ?? "\(amount)")원"
    }
}
