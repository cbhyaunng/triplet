package com.triplet.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PaymentNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        TripletDebugStore.push(
            NotificationDebugEvent(
                packageName = packageName,
                stage = NotificationStage.RECEIVED,
                summary = "Notification listener 연결 완료",
                detail = "테스트 알림 수신 준비가 끝났습니다.",
            ),
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        TripletNotificationProcessor.processPostedNotification(sbn)
    }
}
