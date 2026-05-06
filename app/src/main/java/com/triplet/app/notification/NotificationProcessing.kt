package com.triplet.app.notification

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.triplet.app.travel.MatchedExpense
import com.triplet.app.travel.MatchReviewStatus
import com.triplet.app.travel.TripletTravelStore
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

private const val DEMO_PACKAGE = "com.triplet.demo.notifier"
private const val DEMO_FORMAT_MARKER = "TRIPLET_DEMO_V1"
private const val KEY_VERSION = "triplet.demo.version"
private const val KEY_TX_ID = "triplet.demo.tx_id"
private const val KEY_MERCHANT = "triplet.demo.merchant"
private const val KEY_AMOUNT_MINOR = "triplet.demo.amount_minor"
private const val KEY_CURRENCY = "triplet.demo.currency"
private const val KEY_OCCURRED_AT = "triplet.demo.occurred_at"
private const val KEY_CATEGORY = "triplet.demo.category"
private const val KEY_NOTE = "triplet.demo.note"
private const val KEY_LATITUDE = "triplet.demo.latitude"
private const val KEY_LONGITUDE = "triplet.demo.longitude"

data class ExtractedNotificationPayload(
    val packageName: String,
    val postTime: Long,
    val channelId: String?,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val extras: Bundle,
)

data class ParsedNotificationPayload(
    val txId: String,
    val merchantName: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAt: Instant,
    val category: String?,
    val note: String?,
    val latitude: Double?,
    val longitude: Double?,
)

interface PaymentNotificationParser {
    fun canParse(payload: ExtractedNotificationPayload): Boolean
    fun parse(payload: ExtractedNotificationPayload): ParsedNotificationPayload
}

object NotificationAllowlist {
    private val allowedPackages = setOf(DEMO_PACKAGE)

    fun isAllowed(packageName: String): Boolean = packageName in allowedPackages
}

object NotificationPayloadExtractor {
    fun extract(sbn: StatusBarNotification): ExtractedNotificationPayload {
        val extras = sbn.notification.extras ?: Bundle.EMPTY
        return ExtractedNotificationPayload(
            packageName = sbn.packageName,
            postTime = sbn.postTime,
            channelId = sbn.notification.channelId,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras = extras,
        )
    }
}

object NotificationParserRegistry {
    private val parsers: List<PaymentNotificationParser> = listOf(DemoPaymentNotificationParser)

    fun findParser(payload: ExtractedNotificationPayload): PaymentNotificationParser? {
        return parsers.firstOrNull { it.canParse(payload) }
    }
}

object DemoPaymentNotificationParser : PaymentNotificationParser {
    private val titleAmountRegex = Regex("""(\d[\d,]*)\s*([A-Z]{3})""")

    override fun canParse(payload: ExtractedNotificationPayload): Boolean {
        return payload.packageName == DEMO_PACKAGE &&
            (payload.extras.containsKey(KEY_VERSION) || payload.subText == DEMO_FORMAT_MARKER)
    }

    override fun parse(payload: ExtractedNotificationPayload): ParsedNotificationPayload {
        val kv = parseKeyValue(payload.bigText)
        val txId = payload.extras.getString(KEY_TX_ID)
            ?: kv[KEY_TX_ID.removePrefix("triplet.demo.")]
            ?: "fallback-${payload.postTime}"

        val merchant = payload.extras.getString(KEY_MERCHANT)
            ?: kv["merchant"]
            ?: payload.text?.substringBefore("·")?.trim()
            ?: error("merchant is missing")

        val amountMinor = when {
            payload.extras.containsKey(KEY_AMOUNT_MINOR) -> payload.extras.getLong(KEY_AMOUNT_MINOR)
            kv["amount_minor"] != null -> kv["amount_minor"]!!.replace(",", "").toLong()
            else -> parseAmountMinorFromTitle(payload.title)
        }

        require(amountMinor > 0) { "amount_minor must be greater than 0" }

        val currency = payload.extras.getString(KEY_CURRENCY)
            ?: kv["currency"]
            ?: parseCurrencyFromTitle(payload.title)
            ?: "KRW"

        val occurredAtRaw = payload.extras.getString(KEY_OCCURRED_AT)
            ?: kv["occurred_at"]
            ?: payload.text?.substringAfter("·")?.trim()
            ?: Instant.ofEpochMilli(payload.postTime).toString()

        val occurredAt = parseInstantLenient(occurredAtRaw)
        val category = payload.extras.getString(KEY_CATEGORY) ?: kv["category"]
        val note = payload.extras.getString(KEY_NOTE) ?: kv["note"]
        val latitude = payload.extras.optDoubleOrNull(KEY_LATITUDE) ?: kv["latitude"]?.toDoubleOrNull()
        val longitude = payload.extras.optDoubleOrNull(KEY_LONGITUDE) ?: kv["longitude"]?.toDoubleOrNull()

        return ParsedNotificationPayload(
            txId = txId,
            merchantName = merchant,
            amountMinor = amountMinor,
            currencyCode = currency.uppercase(Locale.US),
            occurredAt = occurredAt,
            category = category,
            note = note,
            latitude = latitude,
            longitude = longitude,
        )
    }

    private fun parseAmountMinorFromTitle(title: String?): Long {
        val match = titleAmountRegex.find(title.orEmpty()) ?: error("amount is missing in title")
        return match.groupValues[1].replace(",", "").toLong()
    }

    private fun parseCurrencyFromTitle(title: String?): String? {
        val match = titleAmountRegex.find(title.orEmpty()) ?: return null
        return match.groupValues[2]
    }

    private fun parseKeyValue(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";")
            .mapNotNull { segment ->
                val parts = segment.split("=", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                parts[0].trim() to parts[1].trim()
            }
            .toMap()
    }

    private fun parseInstantLenient(raw: String): Instant {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrElse { Instant.now() }
    }
}

object TripletNotificationProcessor {
    fun processPostedNotification(sbn: StatusBarNotification) {
        if (!NotificationAllowlist.isAllowed(sbn.packageName)) {
            TripletDebugStore.push(
                NotificationDebugEvent(
                    packageName = sbn.packageName,
                    stage = NotificationStage.IGNORED,
                    summary = "허용되지 않은 앱 알림을 무시했습니다.",
                    detail = "패키지 allowlist에 없는 알림입니다.",
                ),
            )
            return
        }

        if (!TripletTravelStore.travelModeEnabled) {
            TripletDebugStore.push(
                NotificationDebugEvent(
                    packageName = sbn.packageName,
                    stage = NotificationStage.IGNORED,
                    summary = "여행모드가 꺼져 있어 알림을 무시했습니다.",
                    detail = "발표 데모에서는 여행모드가 켜진 상태에서만 결제 후보를 생성합니다.",
                ),
            )
            return
        }

        val extracted = NotificationPayloadExtractor.extract(sbn)
        TripletDebugStore.push(
            NotificationDebugEvent(
                packageName = extracted.packageName,
                stage = NotificationStage.RECEIVED,
                summary = extracted.title ?: "알림 수신",
                detail = buildString {
                    append("channel=")
                    append(extracted.channelId ?: "none")
                    append(" / text=")
                    append(extracted.text ?: "none")
                },
            ),
        )

        val parser = NotificationParserRegistry.findParser(extracted)
        if (parser == null) {
            TripletDebugStore.push(
                NotificationDebugEvent(
                    packageName = extracted.packageName,
                    stage = NotificationStage.ERROR,
                    summary = "parser를 찾지 못했습니다.",
                    detail = "format marker 또는 extras version이 예상과 다릅니다.",
                ),
            )
            return
        }

        runCatching { parser.parse(extracted) }
            .onSuccess { parsed ->
                val hasDemoCoordinates = parsed.latitude != null && parsed.longitude != null
                val locationMatch =
                    if (hasDemoCoordinates) {
                        null
                    } else {
                        TripletTravelStore.findNearestLocation(parsed.occurredAt)
                    }
                val expense =
                    MatchedExpense(
                        txId = parsed.txId,
                        merchantName = parsed.merchantName,
                        amountMinor = parsed.amountMinor,
                        currencyCode = parsed.currencyCode,
                        occurredAt = parsed.occurredAt,
                        category = parsed.category,
                        note = parsed.note,
                        latitude = parsed.latitude ?: locationMatch?.sample?.latitude,
                        longitude = parsed.longitude ?: locationMatch?.sample?.longitude,
                        accuracyM = if (hasDemoCoordinates) null else locationMatch?.sample?.accuracyM,
                        matchConfidence = if (hasDemoCoordinates) 1f else locationMatch?.confidence ?: 0f,
                        reviewStatus =
                            if (hasDemoCoordinates) {
                                MatchReviewStatus.AUTO_CONFIRMED
                            } else {
                                locationMatch?.reviewStatus ?: MatchReviewStatus.NEEDS_REVIEW
                            },
                    )
                TripletTravelStore.addOrReplaceExpense(expense)

                TripletDebugStore.push(
                    NotificationDebugEvent(
                        packageName = extracted.packageName,
                        stage = NotificationStage.PARSED,
                        summary = "${parsed.merchantName} / ${parsed.amountMinor} ${parsed.currencyCode}",
                        detail = buildString {
                            append("tx_id=")
                            append(parsed.txId)
                            append(" / occurred_at=")
                            append(parsed.occurredAt)
                            if (!parsed.category.isNullOrBlank()) {
                                append(" / category=")
                                append(parsed.category)
                            }
                            if (!parsed.note.isNullOrBlank()) {
                                append(" / note=")
                                append(parsed.note)
                            }
                        },
                    ),
                )
                TripletDebugStore.push(
                    NotificationDebugEvent(
                        packageName = extracted.packageName,
                        stage = NotificationStage.MATCHED,
                        summary = if (hasDemoCoordinates) {
                            "시연 좌표 사용"
                        } else if (locationMatch != null) {
                            "위치 매칭 성공: ${"%.2f".format(locationMatch.confidence)}"
                        } else {
                            "위치 매칭 실패"
                        },
                        detail = if (hasDemoCoordinates) {
                            "lat=${parsed.latitude}, lng=${parsed.longitude}, review=${MatchReviewStatus.AUTO_CONFIRMED}"
                        } else if (locationMatch != null) {
                            buildString {
                                append("lat=")
                                append(locationMatch.sample.latitude)
                                append(", lng=")
                                append(locationMatch.sample.longitude)
                                append(", accuracy=")
                                append(locationMatch.sample.accuracyM)
                                append(", diffSec=")
                                append(locationMatch.timeDiffSeconds)
                                append(", review=")
                                append(locationMatch.reviewStatus)
                            }
                        } else {
                            "아직 저장된 위치 샘플이 없어 NEEDS_REVIEW 상태로 기록합니다."
                        },
                    ),
                )
            }
            .onFailure { throwable ->
                TripletDebugStore.push(
                    NotificationDebugEvent(
                        packageName = extracted.packageName,
                        stage = NotificationStage.ERROR,
                        summary = "알림 파싱에 실패했습니다.",
                        detail = throwable.message ?: "unknown error",
                    ),
                )
            }
    }
}

private fun Bundle.optDoubleOrNull(key: String): Double? {
    return if (!containsKey(key)) null else getDouble(key)
}
