package com.triplet.demo.notifier

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

data class DemoPaymentPayload(
    val txId: String,
    val merchantName: String,
    val amountMinor: Long,
    val currencyCode: String,
    val category: String,
    val occurredAt: OffsetDateTime,
    val note: String?,
)

enum class DemoCategory(val code: String) {
    FOOD("FOOD"),
    CAFE("CAFE"),
    TRANSPORT("TRANSPORT"),
    SHOPPING("SHOPPING"),
    ETC("ETC"),
}

object DemoNotificationSender {
    const val channelId: String = "triplet_demo_payment_alerts"
    private const val channelName = "Demo Payment Alerts"
    private const val formatMarker = "TRIPLET_DEMO_V1"

    fun send(context: Context, payload: DemoPaymentPayload) {
        ensureChannel(context)
        val occurredAt = payload.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val amountText = NumberFormat.getIntegerInstance(Locale.KOREA).format(payload.amountMinor)
        val extras = Bundle().apply {
            putInt("triplet.demo.version", 1)
            putString("triplet.demo.tx_id", payload.txId)
            putString("triplet.demo.merchant", payload.merchantName)
            putLong("triplet.demo.amount_minor", payload.amountMinor)
            putString("triplet.demo.currency", payload.currencyCode)
            putString("triplet.demo.occurred_at", occurredAt)
            putString("triplet.demo.category", payload.category)
            putString("triplet.demo.note", payload.note)
        }
        val bigText = buildString {
            append("merchant=${payload.merchantName}")
            append("; amount_minor=${payload.amountMinor}")
            append("; currency=${payload.currencyCode}")
            append("; occurred_at=$occurredAt")
            append("; category=${payload.category}")
            append("; tx_id=${payload.txId}")
            payload.note?.takeIf { it.isNotBlank() }?.let {
                append("; note=$it")
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("[승인] $amountText ${payload.currencyCode}")
            .setContentText("${payload.merchantName} · $occurredAt")
            .setSubText(formatMarker)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .addExtras(extras)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(payload.txId.hashCode(), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Triplet 발표용 결제 승인 테스트 알림"
        }
        manager.createNotificationChannel(channel)
    }
}
