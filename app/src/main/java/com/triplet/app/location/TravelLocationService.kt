package com.triplet.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.triplet.app.R
import com.triplet.app.notification.NotificationDebugEvent
import com.triplet.app.notification.NotificationStage
import com.triplet.app.notification.TripletDebugStore
import com.triplet.app.travel.LocationSample
import com.triplet.app.travel.TripletTravelStore
import java.time.Instant
import java.util.UUID

class TravelLocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isTracking = false

    private val locationCallback =
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    TripletTravelStore.addLocationSample(
                        LocationSample(
                            id = UUID.randomUUID().toString(),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyM = location.accuracy,
                            capturedAt = Instant.ofEpochMilli(location.time),
                            source = "FUSED_UPDATE",
                        ),
                    )
                    TripletDebugStore.push(
                        NotificationDebugEvent(
                            packageName = packageName,
                            stage = NotificationStage.LOCATION,
                            summary = "위치 샘플 저장",
                            detail = "lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}",
                        ),
                    )
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                if (!hasLocationPermission()) {
                    TripletDebugStore.push(
                        NotificationDebugEvent(
                            packageName = packageName,
                            stage = NotificationStage.ERROR,
                            summary = "위치 권한이 없어 여행모드를 시작하지 못했습니다.",
                            detail = "ACCESS_FINE_LOCATION 권한을 먼저 허용해야 합니다.",
                        ),
                    )
                    stopSelf()
                    return START_NOT_STICKY
                }

                createChannelIfNeeded()
                startForeground(
                    NOTIFICATION_ID,
                    NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .setContentTitle("Triplet 여행모드 실행 중")
                        .setContentText("위치 샘플을 수집해 결제 알림과 매칭합니다.")
                        .setOngoing(true)
                        .build(),
                )
                startTracking()
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (isTracking) return
        isTracking = true
        TripletTravelStore.updateLocationServiceActive(true)
        TripletDebugStore.push(
            NotificationDebugEvent(
                packageName = packageName,
                stage = NotificationStage.SYSTEM,
                summary = "여행모드 위치 서비스 시작",
                detail = "foreground service 기반 위치 수집을 시작합니다.",
            ),
        )

        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                TripletTravelStore.addLocationSample(
                    LocationSample(
                        id = UUID.randomUUID().toString(),
                        latitude = lastLocation.latitude,
                        longitude = lastLocation.longitude,
                        accuracyM = lastLocation.accuracy,
                        capturedAt = Instant.ofEpochMilli(lastLocation.time),
                        source = "LAST_KNOWN",
                    ),
                )
            }
        }

        val request =
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 120_000L)
                .setMinUpdateIntervalMillis(30_000L)
                .setMinUpdateDistanceMeters(50f)
                .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper(),
        )
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        runCatching {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        TripletTravelStore.updateLocationServiceActive(false)
        TripletTravelStore.updateTravelModeEnabled(false)
        TripletDebugStore.push(
            NotificationDebugEvent(
                packageName = packageName,
                stage = NotificationStage.SYSTEM,
                summary = "여행모드 위치 서비스 종료",
                detail = "위치 샘플 수집을 중단했습니다.",
            ),
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.location_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.location_channel_description)
            }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "triplet_travel_mode"
        private const val NOTIFICATION_ID = 1024
        private const val ACTION_START = "com.triplet.app.action.START_TRAVEL_LOCATION"
        private const val ACTION_STOP = "com.triplet.app.action.STOP_TRAVEL_LOCATION"

        fun start(context: Context) {
            val intent = Intent(context, TravelLocationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TravelLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
