import ActivityKit
import Foundation

struct TripletTravelActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var isTracking: Bool
        var expenseCount: Int
        var routePointCount: Int
        var totalAmountMinor: Int
        var latestMerchantName: String?
        var latestAmountMinor: Int?
        var lastUpdatedAt: Date
    }

    var tripName: String
    var startedAt: Date
}
