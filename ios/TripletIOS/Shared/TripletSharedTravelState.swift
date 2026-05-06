import Foundation

enum TripletSharedTravelState {
    static let appGroupIdentifier = "group.com.triplet.ios"
    private static let travelModeEnabledKey = "triplet.travelModeEnabled"
    private static let travelModeStartedAtKey = "triplet.travelModeStartedAt"

    static var isTravelModeEnabled: Bool {
        defaults.bool(forKey: travelModeEnabledKey)
    }

    static var startedAt: Date? {
        defaults.object(forKey: travelModeStartedAtKey) as? Date
    }

    static func setTravelModeEnabled(_ isEnabled: Bool, startedAt: Date? = Date()) {
        defaults.set(isEnabled, forKey: travelModeEnabledKey)
        if isEnabled {
            defaults.set(startedAt ?? Date(), forKey: travelModeStartedAtKey)
        } else {
            defaults.removeObject(forKey: travelModeStartedAtKey)
        }
    }

    private static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroupIdentifier) ?? .standard
    }
}
