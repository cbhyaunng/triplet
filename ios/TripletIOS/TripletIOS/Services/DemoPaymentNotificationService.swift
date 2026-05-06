import Foundation
import UserNotifications

enum PaymentNotificationParserError: LocalizedError {
    case missingField(String)
    case invalidValue(String)

    var errorDescription: String? {
        switch self {
        case .missingField(let field):
            return "\(field) 값이 없습니다."
        case .invalidValue(let field):
            return "\(field) 값이 올바르지 않습니다."
        }
    }
}

struct PaymentNotificationParser {
    static func parse(userInfo: [AnyHashable: Any]) throws -> PaymentNotificationPayload {
        let merchant = try stringValue(userInfo, key: "triplet.demo.merchant")
        let txId = try stringValue(userInfo, key: "triplet.demo.tx_id")
        let currency = (try? stringValue(userInfo, key: "triplet.demo.currency")) ?? "KRW"
        let occurredAtRaw = try stringValue(userInfo, key: "triplet.demo.occurred_at")
        guard let occurredAt = TripletFormat.iso8601.date(from: occurredAtRaw) else {
            throw PaymentNotificationParserError.invalidValue("occurred_at")
        }
        let amount = try intValue(userInfo, key: "triplet.demo.amount_minor")
        guard amount > 0 else {
            throw PaymentNotificationParserError.invalidValue("amount_minor")
        }
        let categoryRaw = ((try? stringValue(userInfo, key: "triplet.demo.category")) ?? "ETC").uppercased()
        let category = ExpenseCategory(rawValue: categoryRaw) ?? .etc
        let note = try? stringValue(userInfo, key: "triplet.demo.note")
        let latitude = try? doubleValue(userInfo, key: "triplet.demo.latitude")
        let longitude = try? doubleValue(userInfo, key: "triplet.demo.longitude")

        return PaymentNotificationPayload(
            txId: txId,
            merchantName: merchant,
            amountMinor: amount,
            currencyCode: currency.uppercased(),
            occurredAt: occurredAt,
            category: category,
            note: note,
            latitude: latitude,
            longitude: longitude
        )
    }

    private static func stringValue(_ userInfo: [AnyHashable: Any], key: String) throws -> String {
        guard let value = userInfo[key] as? String, !value.trimmingCharacters(in: .whitespaces).isEmpty else {
            throw PaymentNotificationParserError.missingField(key)
        }
        return value
    }

    private static func intValue(_ userInfo: [AnyHashable: Any], key: String) throws -> Int {
        if let value = userInfo[key] as? Int {
            return value
        }
        if let value = userInfo[key] as? NSNumber {
            return value.intValue
        }
        if let value = userInfo[key] as? String, let parsed = Int(value) {
            return parsed
        }
        throw PaymentNotificationParserError.missingField(key)
    }

    private static func doubleValue(_ userInfo: [AnyHashable: Any], key: String) throws -> Double {
        if let value = userInfo[key] as? Double {
            return value
        }
        if let value = userInfo[key] as? NSNumber {
            return value.doubleValue
        }
        if let value = userInfo[key] as? String, let parsed = Double(value) {
            return parsed
        }
        throw PaymentNotificationParserError.missingField(key)
    }
}

final class DemoPaymentNotificationService {
    func requestAuthorization(completion: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
    }

    func send(payload: PaymentNotificationPayload, delaySeconds: Int, completion: @escaping (Result<Void, Error>) -> Void) {
        let content = UNMutableNotificationContent()
        content.title = "[승인] \(TripletFormat.currency.string(from: NSNumber(value: payload.amountMinor)) ?? "\(payload.amountMinor)") \(payload.currencyCode)"
        content.body = "\(payload.merchantName) · \(TripletFormat.iso8601.string(from: payload.occurredAt))"
        content.sound = .default
        content.userInfo = userInfo(from: payload)

        let trigger: UNNotificationTrigger?
        if delaySeconds > 0 {
            trigger = UNTimeIntervalNotificationTrigger(timeInterval: TimeInterval(delaySeconds), repeats: false)
        } else {
            trigger = nil
        }

        let request = UNNotificationRequest(identifier: payload.txId, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request) { error in
            DispatchQueue.main.async {
                if let error {
                    completion(.failure(error))
                } else {
                    completion(.success(()))
                }
            }
        }
    }

    private func userInfo(from payload: PaymentNotificationPayload) -> [String: Any] {
        var info: [String: Any] = [
            "triplet.demo.version": 1,
            "triplet.demo.tx_id": payload.txId,
            "triplet.demo.merchant": payload.merchantName,
            "triplet.demo.amount_minor": payload.amountMinor,
            "triplet.demo.currency": payload.currencyCode,
            "triplet.demo.occurred_at": TripletFormat.iso8601.string(from: payload.occurredAt),
            "triplet.demo.category": payload.category.rawValue
        ]
        if let note = payload.note {
            info["triplet.demo.note"] = note
        }
        if let latitude = payload.latitude, let longitude = payload.longitude {
            info["triplet.demo.latitude"] = latitude
            info["triplet.demo.longitude"] = longitude
        }
        return info
    }
}
