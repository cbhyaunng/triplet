package com.triplet.app.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.util.UUID

data class NotificationDebugEvent(
    val id: String = UUID.randomUUID().toString(),
    val recordedAt: Instant = Instant.now(),
    val packageName: String,
    val stage: NotificationStage,
    val summary: String,
    val detail: String,
)

enum class NotificationStage(
    val label: String,
    val color: Color,
) {
    SYSTEM("SYSTEM", Color(0xFF6941C6)),
    RECEIVED("RECEIVED", Color(0xFF175CD3)),
    PARSED("PARSED", Color(0xFF027A48)),
    LOCATION("LOCATION", Color(0xFF0086C9)),
    MATCHED("MATCHED", Color(0xFF087443)),
    IGNORED("IGNORED", Color(0xFFB54708)),
    ERROR("ERROR", Color(0xFFB42318)),
}

object TripletDebugStore {
    val events = mutableStateListOf<NotificationDebugEvent>()

    fun push(event: NotificationDebugEvent) {
        if (events.size >= 50) {
            events.removeAt(0)
        }
        events.add(event)
    }

    fun clearEvents() {
        events.clear()
    }
}
