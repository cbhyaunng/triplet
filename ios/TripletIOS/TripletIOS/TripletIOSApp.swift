import SwiftUI

@main
struct TripletIOSApp: App {
    @UIApplicationDelegateAdaptor(NotificationAppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var store = TripletStore()
    @StateObject private var locationService = TravelLocationService()
    private let notificationService = DemoPaymentNotificationService()

    var body: some Scene {
        WindowGroup {
            ContentView(notificationService: notificationService)
                .environmentObject(store)
                .environmentObject(locationService)
                .onAppear {
                    locationService.onSample = { sample in
                        store.addLocationSample(sample)
                    }
                    syncControlCenterTravelMode()
                }
                .onChange(of: scenePhase) { _, newPhase in
                    if newPhase == .active {
                        syncControlCenterTravelMode()
                    }
                }
        }
    }

    private func syncControlCenterTravelMode() {
        guard store.applySharedTravelModeIfNeeded() else { return }

        if store.travelModeEnabled {
            locationService.requestWhenInUse()
            locationService.startTracking()
        } else {
            locationService.stopTracking()
        }
    }
}
