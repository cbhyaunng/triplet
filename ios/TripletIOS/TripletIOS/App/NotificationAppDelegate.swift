import UIKit
import UserNotifications

extension Notification.Name {
    static let tripletDemoPaymentReceived = Notification.Name("tripletDemoPaymentReceived")
}

final class NotificationAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        postPayload(notification.request.content.userInfo)
        completionHandler([.banner, .sound, .list])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        postPayload(response.notification.request.content.userInfo)
        completionHandler()
    }

    private func postPayload(_ userInfo: [AnyHashable: Any]) {
        guard userInfo["triplet.demo.version"] != nil else { return }
        NotificationCenter.default.post(
            name: .tripletDemoPaymentReceived,
            object: nil,
            userInfo: userInfo
        )
    }
}
