package com.triplet.app.map

import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.triplet.app.BuildConfig
import com.triplet.app.travel.LocationSample
import com.triplet.app.travel.MatchedExpense
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val MapGrey50 = Color(0xFFF9FAFB)
private val MapGrey100 = Color(0xFFF2F4F6)
private val MapGrey900 = Color(0xFF191F28)
private val MapBlue50 = Color(0xFFE8F3FF)
private val MapBlue600 = Color(0xFF2272EB)
private val MapOrange50 = Color(0xFFFFF3E0)
private val MapOrange700 = Color(0xFFF57800)

@Composable
fun TripletMapSection(
    modifier: Modifier = Modifier,
    samples: List<LocationSample>,
    expenses: List<MatchedExpense>,
    onOpenFullMap: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFullMap),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "지도 위 소비 기록",
                        style = MaterialTheme.typography.titleLarge,
                        color = MapGrey900,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (BuildConfig.MAPS_API_KEY.isBlank()) {
                            "지도 준비 중"
                        } else {
                            "이동 경로와 결제 위치"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapPill("경로 ${samples.size}점", MapBlue50, MapBlue600)
                MapPill("소비 ${expenses.size}건", MapOrange50, MapOrange700)
            }

            if (BuildConfig.MAPS_API_KEY.isBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f),
                    colors = CardDefaults.cardColors(containerColor = MapGrey50),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("지도 준비 중", fontWeight = FontWeight.Bold, color = MapGrey900)
                        Text(
                            text = "샘플 ${samples.size}개 / 소비 ${expenses.size}건",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MapGrey100, MaterialTheme.shapes.medium)
                        .padding(1.dp),
                ) {
                    GoogleMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f),
                        samples = samples,
                        expenses = expenses,
                        onExpenseSelected = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GoogleMapView(
    modifier: Modifier,
    samples: List<LocationSample>,
    expenses: List<MatchedExpense>,
    onExpenseSelected: (MatchedExpense) -> Unit,
) {
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            mapView.apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                getMapAsync { map ->
                    googleMap = map
                    map.uiSettings.isZoomControlsEnabled = true
                    map.uiSettings.isCompassEnabled = true
                    updateMapContent(mapView, map, samples, expenses, onExpenseSelected)
                }
            }
        },
        update = {
            googleMap?.let { map ->
                updateMapContent(mapView, map, samples, expenses, onExpenseSelected)
            }
        },
    )
}

@Composable
fun TripletFullScreenMap(
    modifier: Modifier = Modifier,
    samples: List<LocationSample>,
    expenses: List<MatchedExpense>,
    onExpenseSelected: (MatchedExpense) -> Unit,
) {
    if (BuildConfig.MAPS_API_KEY.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MapGrey50),
            contentAlignment = Alignment.Center,
        ) {
            Text("지도 준비 중", color = MapGrey900, fontWeight = FontWeight.Bold)
        }
    } else {
        GoogleMapView(
            modifier = modifier,
            samples = samples,
            expenses = expenses,
            onExpenseSelected = onExpenseSelected,
        )
    }
}

private fun updateMapContent(
    mapView: MapView,
    map: GoogleMap,
    samples: List<LocationSample>,
    expenses: List<MatchedExpense>,
    onExpenseSelected: (MatchedExpense) -> Unit,
) {
    map.clear()
    val markerExpenses = mutableMapOf<Marker, MatchedExpense>()

    val routePoints = samples.map { LatLng(it.latitude, it.longitude) }
    val expensePoints =
        expenses.sortedBy { it.occurredAt }.mapNotNull { expense ->
            val lat = expense.latitude
            val lng = expense.longitude
            if (lat != null && lng != null) LatLng(lat, lng) else null
        }
    val anchorPoint = expensePoints.lastOrNull() ?: routePoints.lastOrNull()
    val visibleRoutePoints =
        if (expensePoints.size >= 2) {
            expensePoints
        } else if (anchorPoint != null && routePoints.size > 2) {
            routePoints.filter { point -> distanceMeters(anchorPoint, point) <= 100_000.0 }
        } else {
            routePoints
        }.ifEmpty { routePoints }

    if (visibleRoutePoints.size >= 2) {
        map.addPolyline(
            PolylineOptions()
                .addAll(visibleRoutePoints)
                .color(0xFF3182F6.toInt())
                .width(8f),
        )
    }

    val amountFormat = NumberFormat.getIntegerInstance(Locale.KOREA)
    expenses.forEach { expense ->
        val lat = expense.latitude ?: return@forEach
        val lng = expense.longitude ?: return@forEach
        val marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(lat, lng))
                .title(expense.merchantName)
                .snippet("${amountFormat.format(expense.amountMinor)}원")
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        if (expense.matchConfidence >= 0.7f) {
                            BitmapDescriptorFactory.HUE_ORANGE
                        } else {
                            BitmapDescriptorFactory.HUE_ORANGE
                        },
                    ),
                ),
        )
        if (marker != null) {
            markerExpenses[marker] = expense
        }
    }
    map.setOnMarkerClickListener { marker ->
        markerExpenses[marker]?.let { expense ->
            onExpenseSelected(expense)
            true
        } ?: false
    }

    val allPoints = buildList {
        addAll(visibleRoutePoints)
        addAll(expensePoints)
    }

    when {
        allPoints.size >= 2 -> {
            val boundsBuilder = LatLngBounds.builder()
            allPoints.forEach(boundsBuilder::include)
            val bounds = boundsBuilder.build()
            mapView.post {
                runCatching {
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
                }.onFailure {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(allPoints.first(), 14f))
                }
            }
        }

        allPoints.size == 1 -> {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(allPoints.first(), 15f))
        }

        else -> {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.5665, 126.9780), 11f))
        }
    }
}

private fun distanceMeters(from: LatLng, to: LatLng): Double {
    val earthRadiusM = 6_371_000.0
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val fromLat = Math.toRadians(from.latitude)
    val toLat = Math.toRadians(to.latitude)
    val a =
        sin(dLat / 2).pow(2.0) +
            cos(fromLat) * cos(toLat) * sin(dLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusM * c
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return mapView
}
