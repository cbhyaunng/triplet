package com.triplet.app.travel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.Executors

data class TripletSnapshot(
    val travelModeEnabled: Boolean,
    val locationSamples: List<LocationSample>,
    val matchedExpenses: List<MatchedExpense>,
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
                        put(
                            "locationSamples",
                            JSONArray().apply {
                                snapshot.locationSamples.forEach { sample ->
                                    put(
                                        JSONObject().apply {
                                            put("id", sample.id)
                                            put("latitude", sample.latitude)
                                            put("longitude", sample.longitude)
                                            put("accuracyM", sample.accuracyM.toDouble())
                                            put("capturedAt", sample.capturedAt.toEpochMilli())
                                            put("source", sample.source)
                                        },
                                    )
                                }
                            },
                        )
                        put(
                            "matchedExpenses",
                            JSONArray().apply {
                                snapshot.matchedExpenses.forEach { expense ->
                                    put(
                                        JSONObject().apply {
                                            put("txId", expense.txId)
                                            put("merchantName", expense.merchantName)
                                            put("amountMinor", expense.amountMinor)
                                            put("currencyCode", expense.currencyCode)
                                            put("occurredAt", expense.occurredAt.toEpochMilli())
                                            put("category", expense.category)
                                            put("note", expense.note)
                                            put("latitude", expense.latitude)
                                            put("longitude", expense.longitude)
                                            put("accuracyM", expense.accuracyM?.toDouble())
                                            put("matchConfidence", expense.matchConfidence.toDouble())
                                            put("reviewStatus", expense.reviewStatus.name)
                                        },
                                    )
                                }
                            },
                        )
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
        val locationSamples =
            root.optJSONArray("locationSamples")?.toLocationSamples().orEmpty()
        val matchedExpenses =
            root.optJSONArray("matchedExpenses")?.toMatchedExpenses().orEmpty()

        return TripletSnapshot(
            travelModeEnabled = travelModeEnabled,
            locationSamples = locationSamples,
            matchedExpenses = matchedExpenses,
        )
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
                        reviewStatus = MatchReviewStatus.valueOf(
                            obj.optString("reviewStatus", MatchReviewStatus.NEEDS_REVIEW.name),
                        ),
                    ),
                )
            }
        }
    }
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    return if (!has(key) || isNull(key)) null else optDouble(key)
}
