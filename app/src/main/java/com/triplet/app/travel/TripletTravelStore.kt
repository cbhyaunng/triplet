package com.triplet.app.travel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import java.util.UUID

data class LocationSample(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val capturedAt: Instant,
    val source: String,
)

enum class MatchReviewStatus {
    AUTO_CONFIRMED,
    NEEDS_REVIEW,
}

data class LocationMatchResult(
    val sample: LocationSample,
    val confidence: Float,
    val reviewStatus: MatchReviewStatus,
    val timeDiffSeconds: Long,
)

data class MatchedExpense(
    val txId: String,
    val merchantName: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAt: Instant,
    val category: String?,
    val note: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Float?,
    val matchConfidence: Float,
    val reviewStatus: MatchReviewStatus,
)

object TripletTravelStore {
    var travelModeEnabled by mutableStateOf(false)
    var locationServiceActive by mutableStateOf(false)

    val locationSamples = mutableStateListOf<LocationSample>()
    val matchedExpenses = mutableStateListOf<MatchedExpense>()
    private var suppressPersistence = false

    fun clearTravelData() {
        locationSamples.clear()
        matchedExpenses.clear()
        persistSnapshot()
    }

    fun updateTravelModeEnabled(enabled: Boolean) {
        travelModeEnabled = enabled
        persistSnapshot()
    }

    fun updateLocationServiceActive(active: Boolean) {
        locationServiceActive = active
    }

    fun addLocationSample(sample: LocationSample) {
        val latest = locationSamples.lastOrNull()
        if (latest != null) {
            val sameTimestamp = latest.capturedAt == sample.capturedAt
            val veryClose =
                abs(latest.latitude - sample.latitude) < 0.00001 &&
                    abs(latest.longitude - sample.longitude) < 0.00001
            if (sameTimestamp || veryClose) return
        }
        locationSamples.add(sample)
        if (locationSamples.size > 200) {
            locationSamples.removeAt(0)
        }
        persistSnapshot()
    }

    fun addOrReplaceExpense(expense: MatchedExpense) {
        val existingIndex = matchedExpenses.indexOfFirst { it.txId == expense.txId }
        if (existingIndex >= 0) {
            matchedExpenses[existingIndex] = expense
        } else {
            matchedExpenses.add(expense)
            matchedExpenses.sortBy { it.occurredAt }
        }
        persistSnapshot()
    }

    fun findNearestLocation(target: Instant): LocationMatchResult? {
        val nearest = locationSamples.minByOrNull { sample ->
            abs(sample.capturedAt.toEpochMilli() - target.toEpochMilli())
        } ?: return null

        val timeDiffSeconds = abs(nearest.capturedAt.epochSecond - target.epochSecond)
        val accuracyPenalty = (nearest.accuracyM / 100f).coerceIn(0f, 1f) * 0.45f
        val timePenalty = (timeDiffSeconds / 300f).coerceIn(0f, 1f) * 0.55f
        val confidence = max(0f, 1f - accuracyPenalty - timePenalty)
        val reviewStatus =
            if (timeDiffSeconds <= 300 && nearest.accuracyM <= 50f) {
                MatchReviewStatus.AUTO_CONFIRMED
            } else {
                MatchReviewStatus.NEEDS_REVIEW
            }

        return LocationMatchResult(
            sample = nearest,
            confidence = confidence,
            reviewStatus = reviewStatus,
            timeDiffSeconds = timeDiffSeconds,
        )
    }

    fun restoreSnapshot(snapshot: TripletSnapshot) {
        suppressPersistence = true
        travelModeEnabled = false
        locationServiceActive = false
        locationSamples.clear()
        locationSamples.addAll(snapshot.locationSamples)
        matchedExpenses.clear()
        matchedExpenses.addAll(snapshot.matchedExpenses.sortedBy { it.occurredAt })
        suppressPersistence = false
    }

    fun injectDemoRoute(now: Instant = Instant.now()) {
        locationSamples.clear()
        matchedExpenses.clear()

        val stops =
            listOf(
                DemoStop(
                    merchant = "런던베이글뮤지엄 안국",
                    amountMinor = 18_400,
                    category = "FOOD",
                    latitude = 37.5796,
                    longitude = 126.9864,
                    minutesFromStart = 0,
                ),
                DemoStop(
                    merchant = "아티스트베이커리 안국",
                    amountMinor = 9_800,
                    category = "CAFE",
                    latitude = 37.5774,
                    longitude = 126.9827,
                    minutesFromStart = 42,
                ),
                DemoStop(
                    merchant = "국립현대미술관 서울",
                    amountMinor = 4_000,
                    category = "CULTURE",
                    latitude = 37.5795,
                    longitude = 126.9800,
                    minutesFromStart = 86,
                ),
                DemoStop(
                    merchant = "광장시장 순희네빈대떡",
                    amountMinor = 16_000,
                    category = "FOOD",
                    latitude = 37.5701,
                    longitude = 126.9997,
                    minutesFromStart = 136,
                ),
                DemoStop(
                    merchant = "동대문디자인플라자",
                    amountMinor = 12_600,
                    category = "SHOPPING",
                    latitude = 37.5663,
                    longitude = 127.0095,
                    minutesFromStart = 192,
                ),
            )
        val startAt = now.minusSeconds(stops.last().minutesFromStart * 60L)

        stops.forEachIndexed { index, stop ->
            val occurredAt = startAt.plusSeconds(stop.minutesFromStart * 60L)
            val sample =
                LocationSample(
                    id = UUID.randomUUID().toString(),
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    accuracyM = 14f + index * 3,
                    capturedAt = occurredAt.minusSeconds(40),
                    source = "DEMO_VISIT_${index + 1}",
                )
            addLocationSample(sample)
            addOrReplaceExpense(
                MatchedExpense(
                    txId = "demo-visit-${index + 1}",
                    merchantName = stop.merchant,
                    amountMinor = stop.amountMinor,
                    currencyCode = "KRW",
                    occurredAt = occurredAt,
                    category = stop.category,
                    note = "${index + 1}번째 방문지",
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    accuracyM = sample.accuracyM,
                    matchConfidence = 0.96f,
                    reviewStatus = MatchReviewStatus.AUTO_CONFIRMED,
                ),
            )
        }
    }

    fun totalAmountMinor(): Long = matchedExpenses.sumOf { it.amountMinor }

    fun topCategory(): String? {
        return matchedExpenses
            .groupBy { it.category ?: "ETC" }
            .maxByOrNull { (_, items) -> items.sumOf { it.amountMinor } }
            ?.key
    }

    fun categoryBreakdown(): List<Pair<String, Long>> {
        return matchedExpenses
            .groupBy { it.category ?: "ETC" }
            .mapValues { (_, items) -> items.sumOf { it.amountMinor } }
            .toList()
            .sortedByDescending { it.second }
    }

    private fun persistSnapshot() {
        if (suppressPersistence) return
        TripletPersistence.persist(
            TripletSnapshot(
                travelModeEnabled = travelModeEnabled,
                locationSamples = locationSamples.toList(),
                matchedExpenses = matchedExpenses.toList(),
            ),
        )
    }
}

val TripletTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

private data class DemoStop(
    val merchant: String,
    val amountMinor: Long,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val minutesFromStart: Int,
)
