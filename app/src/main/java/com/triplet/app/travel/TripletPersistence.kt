package com.triplet.app.travel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.Executors

data class TripletSnapshot(
    val travelModeEnabled: Boolean,
    val activeTripStartedAt: Instant?,
    val locationSamples: List<LocationSample>,
    val matchedExpenses: List<MatchedExpense>,
    val archivedTrips: List<TripRecord>,
)

object TripletPersistence {
    private const val FILE_NAME = "triplet_state.json"

    private val ioExecutor = Executors.newSingleThreadExecutor()
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        runCatching { loadSnapshot() }
            .onSuccess { snapshot ->
                if (snapshot != null) {
                    TripletTravelStore.restoreSnapshot(snapshot)
                }
            }
    }

    fun persist(snapshot: TripletSnapshot) {
        val context = appContext ?: return
        ioExecutor.execute {
            runCatching {
                val root =
                    JSONObject().apply {
                        put("travelModeEnabled", snapshot.travelModeEnabled)
                        put("activeTripStartedAt", snapshot.activeTripStartedAt?.toEpochMilli() ?: JSONObject.NULL)
                        put("locationSamples", locationSamplesToJson(snapshot.locationSamples))
                        put("matchedExpenses", matchedExpensesToJson(snapshot.matchedExpenses))
                        put("archivedTrips", tripRecordsToJson(snapshot.archivedTrips))
                    }
                context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { output ->
                    output.write(root.toString().toByteArray())
                }
            }
        }
    }

    private fun loadSnapshot(): TripletSnapshot? {
        val context = appContext ?: return null
        val raw =
            runCatching {
                context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
            }.getOrNull() ?: return null

        val root = JSONObject(raw)
        val travelModeEnabled = root.optBoolean("travelModeEnabled", false)
        val activeTripStartedAt =
            root.optLongOrNull("activeTripStartedAt")?.let { Instant.ofEpochMilli(it) }
        val locationSamples =
            root.optJSONArray("locationSamples")?.toLocationSamples().orEmpty()
        val matchedExpenses =
            root.optJSONArray("matchedExpenses")?.toMatchedExpenses().orEmpty()
        val archivedTrips =
            root.optJSONArray("archivedTrips")?.toTripRecords().orEmpty()

        return TripletSnapshot(
            travelModeEnabled = travelModeEnabled,
            activeTripStartedAt = activeTripStartedAt,
            locationSamples = locationSamples,
            matchedExpenses = matchedExpenses,
            archivedTrips = archivedTrips,
        )
    }

    private fun locationSamplesToJson(samples: List<LocationSample>): JSONArray {
        return JSONArray().apply {
            samples.forEach { sample -> put(sample.toJson()) }
        }
    }

    private fun matchedExpensesToJson(expenses: List<MatchedExpense>): JSONArray {
        return JSONArray().apply {
            expenses.forEach { expense -> put(expense.toJson()) }
        }
    }

    private fun tripRecordsToJson(records: List<TripRecord>): JSONArray {
        return JSONArray().apply {
            records.forEach { record ->
                put(
                    JSONObject().apply {
                        put("id", record.id)
                        put("title", record.title)
                        put("startedAt", record.startedAt.toEpochMilli())
                        put("endedAt", record.endedAt.toEpochMilli())
                        put("locationSamples", locationSamplesToJson(record.locationSamples))
                        put("matchedExpenses", matchedExpensesToJson(record.matchedExpenses))
                    },
                )
            }
        }
    }

    private fun LocationSample.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("latitude", latitude)
            put("longitude", longitude)
            put("accuracyM", accuracyM.toDouble())
            put("capturedAt", capturedAt.toEpochMilli())
            put("source", source)
        }
    }

    private fun MatchedExpense.toJson(): JSONObject {
        return JSONObject().apply {
            put("txId", txId)
            put("merchantName", merchantName)
            put("amountMinor", amountMinor)
            put("currencyCode", currencyCode)
            put("occurredAt", occurredAt.toEpochMilli())
            put("category", category)
            put("note", note)
            put("latitude", latitude)
            put("longitude", longitude)
            put("accuracyM", accuracyM?.toDouble())
            put("matchConfidence", matchConfidence.toDouble())
            put("reviewStatus", reviewStatus.name)
        }
    }

    private fun JSONArray.toLocationSamples(): List<LocationSample> {
        return buildList {
            for (i in 0 until length()) {
                val obj = getJSONObject(i)
                add(
                    LocationSample(
                        id = obj.getString("id"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        accuracyM = obj.getDouble("accuracyM").toFloat(),
                        capturedAt = Instant.ofEpochMilli(obj.getLong("capturedAt")),
                        source = obj.getString("source"),
                    ),
                )
            }
        }
    }

    private fun JSONArray.toMatchedExpenses(): List<MatchedExpense> {
        return buildList {
            for (i in 0 until length()) {
                val obj = getJSONObject(i)
                add(
                    MatchedExpense(
                        txId = obj.getString("txId"),
                        merchantName = obj.getString("merchantName"),
                        amountMinor = obj.getLong("amountMinor"),
                        currencyCode = obj.getString("currencyCode"),
                        occurredAt = Instant.ofEpochMilli(obj.getLong("occurredAt")),
                        category = obj.optString("category").takeIf { it.isNotBlank() },
                        note = obj.optString("note").takeIf { it.isNotBlank() },
                        latitude = obj.optDoubleOrNull("latitude"),
                        longitude = obj.optDoubleOrNull("longitude"),
                        accuracyM = obj.optDoubleOrNull("accuracyM")?.toFloat(),
                        matchConfidence = obj.optDouble("matchConfidence", 0.0).toFloat(),
                        reviewStatus =
                            runCatching {
                                MatchReviewStatus.valueOf(
                                    obj.optString("reviewStatus", MatchReviewStatus.NEEDS_REVIEW.name),
                                )
                            }.getOrDefault(MatchReviewStatus.NEEDS_REVIEW),
                    ),
                )
            }
        }
    }

    private fun JSONArray.toTripRecords(): List<TripRecord> {
        return buildList {
            for (i in 0 until length()) {
                val obj = getJSONObject(i)
                add(
                    TripRecord(
                        id = obj.getString("id"),
                        title = obj.optString("title").takeIf { it.isNotBlank() } ?: "지난 여행",
                        startedAt = Instant.ofEpochMilli(obj.getLong("startedAt")),
                        endedAt = Instant.ofEpochMilli(obj.getLong("endedAt")),
                        locationSamples = obj.optJSONArray("locationSamples")?.toLocationSamples().orEmpty(),
                        matchedExpenses = obj.optJSONArray("matchedExpenses")?.toMatchedExpenses().orEmpty(),
                    ),
                )
            }
        }
    }
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    return if (!has(key) || isNull(key)) null else optDouble(key)
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    return if (!has(key) || isNull(key)) null else optLong(key)
}
