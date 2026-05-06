import CoreLocation
import Foundation
import SwiftUI

enum ExpenseCategory: String, Codable, CaseIterable, Identifiable {
    case food = "FOOD"
    case cafe = "CAFE"
    case shopping = "SHOPPING"
    case culture = "CULTURE"
    case transport = "TRANSPORT"
    case lodging = "LODGING"
    case etc = "ETC"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .food: return "식비"
        case .cafe: return "카페"
        case .shopping: return "쇼핑"
        case .culture: return "문화"
        case .transport: return "교통"
        case .lodging: return "숙박"
        case .etc: return "기타"
        }
    }

    var tint: Color {
        switch self {
        case .food: return TripletColor.blue
        case .cafe: return TripletColor.orange
        case .shopping: return TripletColor.teal
        case .culture: return TripletColor.purple
        case .transport: return TripletColor.green
        case .lodging: return TripletColor.grey600
        case .etc: return TripletColor.grey700
        }
    }
}

enum ExpenseMatchSource: String, Codable {
    case demoCoordinate
    case locationSample
    case manual
    case unmatched

    var displayName: String {
        switch self {
        case .demoCoordinate: return "시연 좌표"
        case .locationSample: return "위치 매칭"
        case .manual: return "직접 선택"
        case .unmatched: return "위치 없음"
        }
    }
}

struct Expense: Identifiable, Codable, Equatable {
    var id: UUID
    var txId: String
    var merchantName: String
    var amountMinor: Int
    var currencyCode: String
    var occurredAt: Date
    var category: ExpenseCategory
    var note: String?
    var latitude: Double?
    var longitude: Double?
    var matchSource: ExpenseMatchSource

    var coordinate: CLLocationCoordinate2D? {
        guard let latitude, let longitude else { return nil }
        return CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

struct LocationSample: Identifiable, Codable, Equatable {
    var id: UUID
    var latitude: Double
    var longitude: Double
    var horizontalAccuracy: Double
    var capturedAt: Date
    var source: String

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

struct TripRecord: Identifiable, Codable, Equatable {
    var id: UUID
    var title: String
    var startedAt: Date
    var endedAt: Date?
    var locationSamples: [LocationSample]
    var expenses: [Expense]

    var totalAmountMinor: Int {
        expenses.reduce(0) { $0 + $1.amountMinor }
    }

    var topCategory: ExpenseCategory? {
        Dictionary(grouping: expenses, by: \.category)
            .map { category, items in
                (category, items.reduce(0) { $0 + $1.amountMinor })
            }
            .max { $0.1 < $1.1 }?
            .0
    }
}

struct PaymentNotificationPayload: Codable {
    var txId: String
    var merchantName: String
    var amountMinor: Int
    var currencyCode: String
    var occurredAt: Date
    var category: ExpenseCategory
    var note: String?
    var latitude: Double?
    var longitude: Double?
}

struct DemoPaymentTemplate: Identifiable {
    var id = UUID()
    var merchantName: String
    var amountMinor: Int
    var category: ExpenseCategory
    var note: String
    var latitude: Double
    var longitude: Double
}

enum TripletTab: String, CaseIterable {
    case home
    case map
    case expenses
    case summary
    case trips

    var title: String {
        switch self {
        case .home: return "홈"
        case .map: return "지도"
        case .expenses: return "소비내역"
        case .summary: return "소비요약"
        case .trips: return "여행관리"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "house.fill"
        case .map: return "map.fill"
        case .expenses: return "receipt.fill"
        case .summary: return "chart.pie.fill"
        case .trips: return "clock.arrow.circlepath"
        }
    }
}

struct DemoTemplates {
    static let all: [DemoPaymentTemplate] = [
        DemoPaymentTemplate(
            merchantName: "런던베이글뮤지엄 안국",
            amountMinor: 18_400,
            category: .food,
            note: "안국 브런치",
            latitude: 37.5796,
            longitude: 126.9864
        ),
        DemoPaymentTemplate(
            merchantName: "아티스트베이커리 안국",
            amountMinor: 9_800,
            category: .cafe,
            note: "커피와 빵",
            latitude: 37.5774,
            longitude: 126.9827
        ),
        DemoPaymentTemplate(
            merchantName: "국립현대미술관 서울",
            amountMinor: 4_000,
            category: .culture,
            note: "전시 관람",
            latitude: 37.5795,
            longitude: 126.9800
        ),
        DemoPaymentTemplate(
            merchantName: "광장시장 순희네빈대떡",
            amountMinor: 16_000,
            category: .food,
            note: "시장 먹거리",
            latitude: 37.5701,
            longitude: 126.9997
        ),
        DemoPaymentTemplate(
            merchantName: "동대문디자인플라자",
            amountMinor: 12_600,
            category: .shopping,
            note: "기념품",
            latitude: 37.5663,
            longitude: 127.0095
        )
    ]
}
