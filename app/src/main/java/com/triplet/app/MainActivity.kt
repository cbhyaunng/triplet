package com.triplet.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.triplet.app.map.TripletMapSection
import com.triplet.app.map.TripletFullScreenMap
import com.triplet.app.map.TripletLocationPickerMap
import com.triplet.app.location.TravelLocationService
import com.triplet.app.notification.NotificationDebugEvent
import com.triplet.app.notification.NotificationStage
import com.triplet.app.notification.TripletDebugStore
import com.triplet.app.travel.LocationSample
import com.triplet.app.travel.MatchedExpense
import com.triplet.app.travel.MatchReviewStatus
import com.triplet.app.travel.TripRecord
import com.triplet.app.travel.TripletTimeFormatter
import com.triplet.app.travel.TripletTravelStore
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TripletDebugApp()
        }
    }
}

private val TossGrey50 = Color(0xFFF9FAFB)
private val TossGrey100 = Color(0xFFF2F4F6)
private val TossGrey200 = Color(0xFFE5E8EB)
private val TossGrey500 = Color(0xFF8B95A1)
private val TossGrey600 = Color(0xFF6B7684)
private val TossGrey700 = Color(0xFF4E5968)
private val TossGrey800 = Color(0xFF333D4B)
private val TossGrey900 = Color(0xFF191F28)
private val TossBlue50 = Color(0xFFE8F3FF)
private val TossBlue500 = Color(0xFF3182F6)
private val TossBlue600 = Color(0xFF2272EB)
private val TossGreen50 = Color(0xFFF0FAF6)
private val TossGreen700 = Color(0xFF029359)
private val TossOrange50 = Color(0xFFFFF3E0)
private val TossOrange700 = Color(0xFFF57800)
private val TossRed600 = Color(0xFFE42939)
private val TossTeal600 = Color(0xFF00A889)
private val TossPurple600 = Color(0xFF7C5CFF)
private val manualExpenseInputFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val tripRecordPeriodFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
private val expenseCategoryOptions = listOf("FOOD", "CAFE", "SHOPPING", "CULTURE", "TRANSPORT", "LODGING", "ETC")

private fun displayCategory(category: String?): String {
    return when (category?.uppercase(Locale.US)) {
        "FOOD" -> "식비"
        "CAFE" -> "카페"
        "SHOPPING" -> "쇼핑"
        "CULTURE" -> "문화"
        "TRANSPORT" -> "교통"
        "LODGING" -> "숙박"
        "ETC" -> "기타"
        null, "" -> "기타"
        else -> category
    }
}

private fun formatManualExpenseDateTime(instant: Instant): String {
    return manualExpenseInputFormatter.format(
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()),
    )
}

private fun parseManualExpenseDateTime(raw: String): Instant? {
    val normalized = raw.trim().replace('.', '-')
    return runCatching {
        LocalDateTime
            .parse(normalized, manualExpenseInputFormatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    }.getOrNull()
}

private fun formatTripRecordPeriod(record: TripRecord): String {
    val zone = ZoneId.systemDefault()
    val start = tripRecordPeriodFormatter.format(LocalDateTime.ofInstant(record.startedAt, zone))
    val end = tripRecordPeriodFormatter.format(LocalDateTime.ofInstant(record.endedAt, zone))
    return "$start - $end"
}

private fun tripTotalAmountMinor(record: TripRecord): Long {
    return record.matchedExpenses.sumOf { it.amountMinor }
}

private fun tripTopCategory(record: TripRecord): String? {
    return record.matchedExpenses
        .groupBy { it.category ?: "ETC" }
        .maxByOrNull { (_, expenses) -> expenses.sumOf { it.amountMinor } }
        ?.key
}

private fun tripCategoryBreakdown(record: TripRecord): List<Pair<String, Long>> {
    return record.matchedExpenses
        .groupBy { it.category ?: "ETC" }
        .mapValues { (_, expenses) -> expenses.sumOf { it.amountMinor } }
        .toList()
        .sortedByDescending { it.second }
}

private enum class TripletTab(
    val label: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    Home("홈", "Triplet", "여행 소비 지도", Icons.Rounded.Home),
    Map("지도", "지도", "이동 경로와 결제 위치", Icons.Rounded.Map),
    Expenses("소비내역", "소비 내역", "결제 기록 전체 보기", Icons.AutoMirrored.Rounded.ReceiptLong),
    Summary("소비요약", "소비 요약", "카테고리별 지출 분석", Icons.Rounded.PieChart),
    TravelManage("여행관리", "여행관리", "저장된 지난 여행", Icons.Rounded.History),
    Settings("설정", "설정", "권한과 시연 상태 관리", Icons.Rounded.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripletDebugApp() {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var pendingTravelModeStart by rememberSaveable { mutableStateOf(false) }

    val events = TripletDebugStore.events.toList().reversed()
    val expenses = TripletTravelStore.matchedExpenses.toList().sortedByDescending { it.occurredAt }
    val locationSamples = TripletTravelStore.locationSamples.toList().sortedByDescending { it.capturedAt }
    val archivedTrips = TripletTravelStore.archivedTrips.toList().sortedByDescending { it.endedAt }
    val totalAmountMinor = TripletTravelStore.totalAmountMinor()
    val topCategory = TripletTravelStore.topCategory()
    val categoryBreakdown = TripletTravelStore.categoryBreakdown()
    val travelModeEnabled = TripletTravelStore.travelModeEnabled
    val locationServiceActive = TripletTravelStore.locationServiceActive
    val locationGranted = remember(refreshTick, travelModeEnabled, locationServiceActive) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
    val listenerGranted = remember(refreshTick) { isNotificationListenerEnabled(context) }

    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingTravelModeStart) {
            startTravelMode(context)
        }
        pendingTravelModeStart = false
        refreshTick++
    }
    var fullScreenMap by rememberSaveable { mutableStateOf(false) }
    var fullScreenLocationPicker by rememberSaveable { mutableStateOf(false) }
    var selectedMapExpense by remember { mutableStateOf<MatchedExpense?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(TripletTab.Home) }
    var selectedTripRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var fullScreenTripRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var manualExpenseEntryOpen by rememberSaveable { mutableStateOf(false) }
    var manualSelectedLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var manualSelectedLongitude by rememberSaveable { mutableStateOf<Double?>(null) }

    fun requestOrStartTravelMode() {
        if (locationGranted) {
            startTravelMode(context)
        } else {
            pendingTravelModeStart = true
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        refreshTick++
    }

    fun stopTravelModeAndRefresh() {
        val archivedTrip = TripletTravelStore.finishActiveTrip(Instant.now())
        stopTravelMode(context)
        if (archivedTrip != null) {
            TripletDebugStore.push(
                NotificationDebugEvent(
                    packageName = context.packageName,
                    stage = NotificationStage.SYSTEM,
                    summary = "여행 기록 저장 완료",
                    detail = "${archivedTrip.title} · 결제 ${archivedTrip.matchedExpenses.size}건",
                ),
            )
            selectedTripRecordId = archivedTrip.id
            selectedTab = TripletTab.TravelManage
        }
        refreshTick++
    }

    fun injectDemoRouteAndRefresh() {
        TripletTravelStore.injectDemoRoute(Instant.now())
        TripletDebugStore.push(
            NotificationDebugEvent(
                packageName = context.packageName,
                stage = NotificationStage.SYSTEM,
                summary = "데모 경로 주입 완료",
                detail = "서울 5개 방문지와 30~60분 간격 결제 내역을 추가했습니다.",
            ),
        )
        refreshTick++
    }

    fun clearDemoDataAndRefresh() {
        TripletTravelStore.clearTravelData()
        TripletDebugStore.clearEvents()
        refreshTick++
    }

    fun addManualExpenseAndRefresh(
        merchantName: String,
        amountMinor: Long,
        occurredAt: Instant,
        category: String,
        note: String?,
        latitude: Double?,
        longitude: Double?,
    ) {
        val expense =
            TripletTravelStore.addManualExpense(
                merchantName = merchantName,
                amountMinor = amountMinor,
                occurredAt = occurredAt,
                category = category,
                note = note,
                latitude = latitude,
                longitude = longitude,
            )
        TripletDebugStore.push(
            NotificationDebugEvent(
                packageName = context.packageName,
                stage = NotificationStage.MATCHED,
                summary = "수동 소비 입력 완료",
                detail = "${expense.merchantName} ${expense.amountMinor}원 저장",
            ),
        )
        manualExpenseEntryOpen = false
        manualSelectedLatitude = null
        manualSelectedLongitude = null
        selectedTab = TripletTab.Expenses
        refreshTick++
    }

    TripletAppTheme {
        if (fullScreenMap) {
            val fullScreenTripRecord =
                fullScreenTripRecordId?.let { tripId ->
                    archivedTrips.firstOrNull { it.id == tripId }
                }
            FullScreenMapPage(
                samples = fullScreenTripRecord?.locationSamples ?: locationSamples,
                expenses = fullScreenTripRecord?.matchedExpenses?.sortedByDescending { it.occurredAt } ?: expenses,
                selectedExpense = selectedMapExpense,
                onBack = {
                    fullScreenMap = false
                    fullScreenTripRecordId = null
                    selectedMapExpense = null
                },
                onExpenseSelected = { selectedMapExpense = it },
            )
            return@TripletAppTheme
        }
        if (fullScreenLocationPicker) {
            FullScreenLocationPickerPage(
                initialLatitude = manualSelectedLatitude ?: locationSamples.firstOrNull()?.latitude ?: 37.5665,
                initialLongitude = manualSelectedLongitude ?: locationSamples.firstOrNull()?.longitude ?: 126.9780,
                onBack = { fullScreenLocationPicker = false },
                onLocationConfirmed = { latitude, longitude ->
                    manualSelectedLatitude = latitude
                    manualSelectedLongitude = longitude
                    manualExpenseEntryOpen = true
                    selectedTab = TripletTab.Expenses
                    fullScreenLocationPicker = false
                },
            )
            return@TripletAppTheme
        }
        Scaffold(
            containerColor = TossGrey100,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(selectedTab.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = selectedTab.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TossGrey100,
                        titleContentColor = TossGrey900,
                    ),
                )
            },
            bottomBar = {
                TripletBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .background(TossGrey100)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = paddingValues.calculateTopPadding() + 10.dp,
                    end = 20.dp,
                    bottom = 18.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    TripletTab.Home -> {
                        item {
                            TripletHeroCard(
                                totalAmountMinor = totalAmountMinor,
                                expenseCount = expenses.size,
                                travelModeEnabled = travelModeEnabled,
                                locationServiceActive = locationServiceActive,
                                onStart = { requestOrStartTravelMode() },
                                onStop = { stopTravelModeAndRefresh() },
                            )
                        }

                        item {
                            TripletMapSection(
                                samples = locationSamples,
                                expenses = expenses,
                                onOpenFullMap = {
                                    selectedMapExpense = null
                                    fullScreenTripRecordId = null
                                    fullScreenMap = true
                                },
                            )
                        }

                        item {
                            SectionTitleRow(
                                title = "소비 내역",
                                subtitle = "최근 결제순",
                                actionLabel = if (expenses.size > 2) "더보기" else null,
                                onAction = { selectedTab = TripletTab.Expenses },
                            )
                        }

                        if (expenses.isEmpty()) {
                            item { EmptyExpenseCard() }
                        } else {
                            items(expenses.take(2), key = { it.txId }) { expense ->
                                ExpenseCard(expense)
                            }
                        }

                        item {
                            StatsSummaryCard(
                                totalAmountMinor = totalAmountMinor,
                                expenseCount = expenses.size,
                                topCategory = topCategory,
                                categoryBreakdown = categoryBreakdown,
                            )
                        }
                    }

                    TripletTab.Map -> {
                        item {
                            TripletMapSection(
                                samples = locationSamples,
                                expenses = expenses,
                                onOpenFullMap = {
                                    selectedMapExpense = null
                                    fullScreenTripRecordId = null
                                    fullScreenMap = true
                                },
                            )
                        }
                    }

                    TripletTab.Expenses -> {
                        item {
                            SectionTitleRow(
                                title = "소비 내역",
                                subtitle = "전체 ${expenses.size}건",
                                actionLabel = if (manualExpenseEntryOpen) "입력 닫기" else "수동 입력",
                                onAction = { manualExpenseEntryOpen = !manualExpenseEntryOpen },
                            )
                        }
                        if (manualExpenseEntryOpen) {
                            item {
                                ManualExpenseEntryCard(
                                    selectedLatitude = manualSelectedLatitude,
                                    selectedLongitude = manualSelectedLongitude,
                                    onOpenLocationPicker = { fullScreenLocationPicker = true },
                                    onClearSelectedLocation = {
                                        manualSelectedLatitude = null
                                        manualSelectedLongitude = null
                                    },
                                    onSave = { merchantName, amountMinor, occurredAt, category, note, latitude, longitude ->
                                        addManualExpenseAndRefresh(
                                            merchantName = merchantName,
                                            amountMinor = amountMinor,
                                            occurredAt = occurredAt,
                                            category = category,
                                            note = note,
                                            latitude = latitude,
                                            longitude = longitude,
                                        )
                                    },
                                )
                            }
                        }
                        if (expenses.isEmpty()) {
                            item { EmptyExpenseCard() }
                        } else {
                            items(expenses, key = { it.txId }) { expense ->
                                ExpenseCard(expense)
                            }
                        }
                    }

                    TripletTab.Summary -> {
                        item {
                            StatsSummaryCard(
                                totalAmountMinor = totalAmountMinor,
                                expenseCount = expenses.size,
                                topCategory = topCategory,
                                categoryBreakdown = categoryBreakdown,
                            )
                        }
                    }

                    TripletTab.TravelManage -> {
                        val selectedTrip =
                            selectedTripRecordId?.let { tripId ->
                                archivedTrips.firstOrNull { it.id == tripId }
                            }
                        if (selectedTrip == null) {
                            item {
                                SectionTitle(
                                    title = "지난 여행",
                                    subtitle = "저장된 여행 ${archivedTrips.size}개",
                                )
                            }
                            if (archivedTrips.isEmpty()) {
                                item { EmptyTripRecordCard() }
                            } else {
                                items(archivedTrips, key = { it.id }) { trip ->
                                    TripRecordCard(
                                        record = trip,
                                        onClick = { selectedTripRecordId = trip.id },
                                    )
                                }
                            }
                        } else {
                            item {
                                SectionTitleRow(
                                    title = selectedTrip.title,
                                    subtitle = formatTripRecordPeriod(selectedTrip),
                                    actionLabel = "목록",
                                    onAction = { selectedTripRecordId = null },
                                )
                            }
                            item {
                                TripRecordSummaryCard(record = selectedTrip)
                            }
                            item {
                                TripletMapSection(
                                    samples = selectedTrip.locationSamples,
                                    expenses = selectedTrip.matchedExpenses.sortedByDescending { it.occurredAt },
                                    onOpenFullMap = {
                                        selectedMapExpense = null
                                        fullScreenTripRecordId = selectedTrip.id
                                        fullScreenMap = true
                                    },
                                )
                            }
                            item {
                                StatsSummaryCard(
                                    totalAmountMinor = tripTotalAmountMinor(selectedTrip),
                                    expenseCount = selectedTrip.matchedExpenses.size,
                                    topCategory = tripTopCategory(selectedTrip),
                                    categoryBreakdown = tripCategoryBreakdown(selectedTrip),
                                )
                            }
                            item {
                                SectionTitle(
                                    title = "여행 소비 내역",
                                    subtitle = "결제 ${selectedTrip.matchedExpenses.size}건",
                                )
                            }
                            if (selectedTrip.matchedExpenses.isEmpty()) {
                                item { EmptyExpenseCard() }
                            } else {
                                items(
                                    selectedTrip.matchedExpenses.sortedByDescending { it.occurredAt },
                                    key = { it.txId },
                                ) { expense ->
                                    ExpenseCard(expense)
                                }
                            }
                        }
                    }

                    TripletTab.Settings -> {
                        item {
                            StatusCard(
                                title = "시연 설정",
                                body = "여행모드를 켜고 테스트 결제를 보내세요.",
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(
                                                text = if (travelModeEnabled) "여행모드 켜짐" else "여행모드 꺼짐",
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = if (locationServiceActive) {
                                                    "위치 기록 중입니다."
                                                } else {
                                                    "위치 기록이 멈춰 있습니다."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Switch(
                                            checked = travelModeEnabled,
                                            onCheckedChange = { enabled ->
                                                if (enabled) {
                                                    requestOrStartTravelMode()
                                                } else {
                                                    stopTravelModeAndRefresh()
                                                }
                                            },
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(
                                            onClick = { requestOrStartTravelMode() },
                                            colors = ButtonDefaults.buttonColors(containerColor = TossBlue500),
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("여행모드 시작")
                                        }
                                        OutlinedButton(onClick = { stopTravelModeAndRefresh() }) {
                                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("여행모드 종료")
                                        }
                                        OutlinedButton(
                                            onClick = { clearDemoDataAndRefresh() },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TossRed600),
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("데이터 초기화")
                                        }
                                    }
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        SummaryMetric("위치 샘플", locationSamples.size.toString())
                                        SummaryMetric("매칭된 결제", expenses.size.toString())
                                        SummaryMetric("로그", events.size.toString())
                                    }
                                }
                            }
                        }

                        item {
                            StatusCard(
                                title = "시연 준비 상태",
                                body = "알림 접근과 위치 권한을 확인하세요.",
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatusRow("알림 접근", listenerGranted)
                                    StatusRow("위치 권한", locationGranted)
                                    StatusRow("위치 서비스", locationServiceActive)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TossGrey900),
                                        ) {
                                            Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("알림 접근 열기")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                            },
                                        ) {
                                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("위치 권한 요청")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { refreshTick++ }) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("상태 새로고침")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            StatusCard(
                                title = "결제 알림 출처",
                                body = "테스트 알림 앱만 읽도록 설정되어 있습니다.",
                            ) {
                                Text(
                                    text = "com.triplet.demo.notifier",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        item { SectionTitle("위치 기록", "최근 수집순") }

                        if (locationSamples.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "여행모드를 시작하면 위치 샘플이 이곳에 쌓입니다.",
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        } else {
                            items(locationSamples.take(10), key = { it.id }) { sample ->
                                LocationSampleCard(sample)
                            }
                        }

                        item { SectionTitle("시연 로그", "처리 상태") }

                        if (events.isEmpty()) {
                            item { EmptyStateCard() }
                        } else {
                            items(items = events, key = { it.id }) { event ->
                                NotificationEventCard(event = event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripletAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TossBlue500,
            onPrimary = Color.White,
            primaryContainer = TossBlue50,
            onPrimaryContainer = TossBlue600,
            secondary = TossBlue500,
            onSecondary = Color.White,
            secondaryContainer = TossBlue50,
            onSecondaryContainer = TossBlue600,
            background = TossGrey100,
            onBackground = TossGrey900,
            surface = Color.White,
            onSurface = TossGrey900,
            surfaceVariant = TossGrey50,
            onSurfaceVariant = TossGrey600,
            outline = TossGrey200,
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(8.dp),
        ),
        content = content,
    )
}

@Composable
private fun TripletBottomNavigationBar(
    selectedTab: TripletTab,
    onTabSelected: (TripletTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TossGrey100)
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(34.dp)),
            color = Color.White,
            shape = RoundedCornerShape(34.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TripletTab.values().forEach { tab ->
                    TripletBottomNavigationItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TripletBottomNavigationItem(
    tab: TripletTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) TossGrey900 else TossGrey500
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            modifier = Modifier.size(24.dp),
            tint = contentColor,
        )
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun FullScreenMapPage(
    samples: List<LocationSample>,
    expenses: List<MatchedExpense>,
    selectedExpense: MatchedExpense?,
    onBack: () -> Unit,
    onExpenseSelected: (MatchedExpense) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TripletFullScreenMap(
            modifier = Modifier.fillMaxSize(),
            samples = samples,
            expenses = expenses,
            selectedExpenseTxId = selectedExpense?.txId,
            onExpenseSelected = onExpenseSelected,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 48.dp)
                .shadow(12.dp, CircleShape),
            color = Color.White,
            contentColor = TossGrey900,
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
            }
        }

        MapExpenseBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            expense = selectedExpense,
        )
    }
}

@Composable
private fun FullScreenLocationPickerPage(
    initialLatitude: Double,
    initialLongitude: Double,
    onBack: () -> Unit,
    onLocationConfirmed: (Double, Double) -> Unit,
) {
    var pickerLatitude by rememberSaveable { mutableStateOf(initialLatitude) }
    var pickerLongitude by rememberSaveable { mutableStateOf(initialLongitude) }
    var confirmDialogOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        TripletLocationPickerMap(
            modifier = Modifier.fillMaxSize(),
            latitude = pickerLatitude,
            longitude = pickerLongitude,
            onLocationSelected = { latitude, longitude ->
                pickerLatitude = latitude
                pickerLongitude = longitude
                confirmDialogOpen = true
            },
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 48.dp)
                .shadow(12.dp, CircleShape),
            color = Color.White,
            contentColor = TossGrey900,
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                .shadow(16.dp, MaterialTheme.shapes.large),
            color = Color.White,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "지도에서 장소 선택",
                    style = MaterialTheme.typography.titleMedium,
                    color = TossGrey900,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "결제한 장소를 지도에서 탭하거나 핀을 움직여 선택하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TossGrey600,
                )
            }
        }
    }

    if (confirmDialogOpen) {
        AlertDialog(
            onDismissRequest = { confirmDialogOpen = false },
            title = { Text("이 장소로 선택하시겠습니까?") },
            text = { Text("선택한 위치가 수동 소비 입력의 결제 장소로 저장됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDialogOpen = false
                        onLocationConfirmed(pickerLatitude, pickerLongitude)
                    },
                ) {
                    Text("예")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDialogOpen = false }) {
                    Text("아니요")
                }
            },
        )
    }
}

@Composable
private fun MapExpenseBottomSheet(
    modifier: Modifier = Modifier,
    expense: MatchedExpense?,
) {
    val amountFormat = remember { NumberFormat.getIntegerInstance(Locale.KOREA) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
            .shadow(16.dp, MaterialTheme.shapes.large),
        color = Color.White,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (expense == null) {
                Text(
                    text = "핀을 선택해보세요",
                    style = MaterialTheme.typography.titleMedium,
                    color = TossGrey900,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "결제 장소와 금액이 여기에 표시됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TossGrey600,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.merchantName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TossGrey900,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = TripletTimeFormatter.format(expense.occurredAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = TossGrey600,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = "${amountFormat.format(expense.amountMinor)}원",
                        style = MaterialTheme.typography.titleMedium,
                        color = TossGrey900,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!expense.category.isNullOrBlank()) {
                    InfoTag(
                        label = displayCategory(expense.category),
                        containerColor = TossBlue50,
                        contentColor = TossBlue600,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseListPage(
    expenses: List<MatchedExpense>,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = TossGrey100,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                title = {
                    Column {
                        Text("소비 내역", fontWeight = FontWeight.Bold)
                        Text(
                            text = "전체 ${expenses.size}건",
                            style = MaterialTheme.typography.bodySmall,
                            color = TossGrey600,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TossGrey100,
                    titleContentColor = TossGrey900,
                    navigationIconContentColor = TossGrey900,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(TossGrey100),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = paddingValues.calculateTopPadding() + 12.dp,
                end = 20.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (expenses.isEmpty()) {
                item {
                    EmptyExpenseCard()
                }
            } else {
                items(expenses, key = { it.txId }) { expense ->
                    ExpenseCard(expense)
                }
            }
        }
    }
}

@Composable
private fun TripletHeroCard(
    totalAmountMinor: Long,
    expenseCount: Int,
    travelModeEnabled: Boolean,
    locationServiceActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val amountFormat = remember { NumberFormat.getIntegerInstance(Locale.KOREA) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "이번 여행 소비",
                        color = TossGrey700,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${amountFormat.format(totalAmountMinor)}원",
                        color = TossGrey900,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "결제 ${expenseCount}건",
                        color = TossGrey500,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                StatusPill(
                    label = when {
                        travelModeEnabled && locationServiceActive -> "여행 중"
                        travelModeEnabled -> "준비"
                        else -> "대기"
                    },
                    active = travelModeEnabled && locationServiceActive,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (travelModeEnabled) {
                            onStop()
                        } else {
                            onStart()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (travelModeEnabled) TossGrey900 else TossBlue500,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = if (travelModeEnabled) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (travelModeEnabled) "여행 중지" else "여행 시작")
                }
            }
        }
    }
}

@Composable
private fun TripRecordCard(
    record: TripRecord,
    onClick: () -> Unit,
) {
    val amountFormat = remember { NumberFormat.getIntegerInstance(Locale.KOREA) }
    val totalAmount = tripTotalAmountMinor(record)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, TossGrey200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TossGrey900,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatTripRecordPeriod(record),
                        style = MaterialTheme.typography.bodySmall,
                        color = TossGrey600,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Text(
                    text = "${amountFormat.format(totalAmount)}원",
                    style = MaterialTheme.typography.titleMedium,
                    color = TossGrey900,
                    fontWeight = FontWeight.Black,
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoTag(
                    label = "결제 ${record.matchedExpenses.size}건",
                    containerColor = TossBlue50,
                    contentColor = TossBlue600,
                )
                InfoTag(
                    label = "경로 ${record.locationSamples.size}점",
                    containerColor = TossGrey100,
                    contentColor = TossGrey600,
                )
                InfoTag(
                    label = tripTopCategory(record)?.let(::displayCategory) ?: "분석 대기",
                    containerColor = TossOrange50,
                    contentColor = TossOrange700,
                )
            }

            Text(
                text = "자세히 보기",
                style = MaterialTheme.typography.bodySmall,
                color = TossBlue600,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TripRecordSummaryCard(record: TripRecord) {
    val amountFormat = remember { NumberFormat.getIntegerInstance(Locale.KOREA) }
    StatusCard(
        title = "여행 요약",
        body = "종료된 여행의 경로와 소비를 다시 볼 수 있습니다.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryMetric("총 소비", "${amountFormat.format(tripTotalAmountMinor(record))}원")
            SummaryMetric("결제", "${record.matchedExpenses.size}건")
            SummaryMetric("경로", "${record.locationSamples.size}점")
        }
    }
}

@Composable
private fun EmptyTripRecordCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("아직 저장된 여행이 없습니다.", fontWeight = FontWeight.Bold)
            Text(
                "여행모드를 켜고 결제 내역을 기록한 뒤 여행 중지를 누르면 이곳에 자동 저장됩니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = TossGrey50,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.padding(horizontal = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TossGrey900,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TossGrey500,
            )
        }
    }
}

@Composable
private fun StatusPill(label: String, active: Boolean) {
    Surface(
        color = if (active) TossBlue50 else TossGrey100,
        contentColor = if (active) TossBlue600 else TossGrey600,
        shape = CircleShape,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TossGrey900,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitleRow(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(title = title, subtitle = subtitle)
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = TossBlue600,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(TossBlue50, CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun InfoTag(
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
private fun ManualExpenseEntryCard(
    selectedLatitude: Double?,
    selectedLongitude: Double?,
    onOpenLocationPicker: () -> Unit,
    onClearSelectedLocation: () -> Unit,
    onSave: (String, Long, Instant, String, String?, Double?, Double?) -> Unit,
) {
    var merchantName by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var dateTimeText by rememberSaveable { mutableStateOf(formatManualExpenseDateTime(Instant.now())) }
    var selectedCategory by rememberSaveable { mutableStateOf("FOOD") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    StatusCard(
        title = "소비 수동 입력",
        body = "현금 결제나 누락된 결제를 직접 추가합니다.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "장소명과 금액을 입력한 뒤, 필요하면 지도에서 결제 위치를 직접 선택할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = TossGrey600,
            )
            OutlinedTextField(
                value = merchantName,
                onValueChange = {
                    merchantName = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("장소") },
                placeholder = { Text("예: 광장시장 순희네빈대떡") },
                singleLine = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenLocationPicker,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("지도에서 장소 선택")
                }
                if (selectedLatitude != null && selectedLongitude != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "지도에서 선택한 장소가 연결되었습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TossBlue600,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "해제",
                            modifier = Modifier.clickable(onClick = onClearSelectedLocation),
                            style = MaterialTheme.typography.bodySmall,
                            color = TossGrey600,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("금액") },
                placeholder = { Text("예: 12000") },
                suffix = { Text("원") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = dateTimeText,
                onValueChange = {
                    dateTimeText = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("날짜시간") },
                placeholder = { Text("yyyy-MM-dd HH:mm") },
                singleLine = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "카테고리",
                    style = MaterialTheme.typography.labelLarge,
                    color = TossGrey800,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    expenseCategoryOptions.forEach { category ->
                        ManualCategoryChip(
                            category = category,
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = category
                                errorMessage = null
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("메모") },
                placeholder = { Text("선택 입력") },
                minLines = 2,
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TossRed600,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Button(
                onClick = {
                    val amountMinor = amountText.filter { it.isDigit() }.toLongOrNull()
                    val occurredAt = parseManualExpenseDateTime(dateTimeText)
                    when {
                        merchantName.isBlank() -> errorMessage = "장소를 입력해주세요."
                        amountMinor == null || amountMinor <= 0L -> errorMessage = "금액을 숫자로 입력해주세요."
                        occurredAt == null -> errorMessage = "날짜시간은 yyyy-MM-dd HH:mm 형식으로 입력해주세요."
                        else -> {
                            onSave(
                                merchantName.trim(),
                                amountMinor,
                                occurredAt,
                                selectedCategory,
                                noteText.trim().takeIf { it.isNotBlank() },
                                selectedLatitude,
                                selectedLongitude,
                            )
                            merchantName = ""
                            amountText = ""
                            dateTimeText = formatManualExpenseDateTime(Instant.now())
                            selectedCategory = "FOOD"
                            noteText = ""
                            onClearSelectedLocation()
                            errorMessage = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TossBlue500),
            ) {
                Text("소비 추가")
            }
        }
    }
}

@Composable
private fun ManualCategoryChip(
    category: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) TossGrey900 else TossGrey100
    val contentColor = if (selected) Color.White else TossGrey700
    Box(
        modifier = Modifier
            .background(containerColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(
            text = displayCategory(category),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatsSummaryCard(
    totalAmountMinor: Long,
    expenseCount: Int,
    topCategory: String?,
    categoryBreakdown: List<Pair<String, Long>>,
) {
    val amountFormat = remember { NumberFormat.getIntegerInstance(Locale.KOREA) }
    StatusCard(
        title = "소비 요약",
        body = "카테고리별 지출",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryMetric("총 결제", "${amountFormat.format(totalAmountMinor)}")
                SummaryMetric("건수", expenseCount.toString())
                SummaryMetric("상위 카테고리", topCategory?.let(::displayCategory) ?: "-")
            }
            HorizontalDivider()
            if (categoryBreakdown.isEmpty()) {
                Text(
                    text = "아직 집계할 소비 데이터가 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategorySpendingChart(
                        categoryBreakdown = categoryBreakdown,
                        totalAmountMinor = totalAmountMinor,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categoryBreakdown.forEach { (category, amount) ->
                            CategoryChip(label = "${displayCategory(category)} ${amountFormat.format(amount)}원")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySpendingChart(
    categoryBreakdown: List<Pair<String, Long>>,
    totalAmountMinor: Long,
) {
    val amountFormat = remember { NumberFormat.getIntegerInstance(Locale.KOREA) }
    val chartTotal = when {
        totalAmountMinor > 0L -> totalAmountMinor
        else -> categoryBreakdown.sumOf { it.second }.coerceAtLeast(1L)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "분야별 지출 그래프",
            style = MaterialTheme.typography.labelLarge,
            color = TossGrey900,
            fontWeight = FontWeight.Bold,
        )
        categoryBreakdown.forEach { (category, amount) ->
            CategorySpendingBar(
                category = category,
                amount = amount,
                totalAmountMinor = chartTotal,
                amountFormat = amountFormat,
            )
        }
    }
}

@Composable
private fun CategorySpendingBar(
    category: String,
    amount: Long,
    totalAmountMinor: Long,
    amountFormat: NumberFormat,
) {
    val fraction = if (totalAmountMinor > 0L) {
        amount.toFloat() / totalAmountMinor.toFloat()
    } else {
        0f
    }
    val percent = (fraction * 100).roundToInt()
    val barFraction = if (fraction > 0f) fraction.coerceIn(0.06f, 1f) else 0f
    val graphColor = categoryGraphColor(category)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(graphColor, CircleShape),
                )
                Text(
                    text = displayCategory(category),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TossGrey800,
                )
            }
            Text(
                text = "${amountFormat.format(amount)}원 · ${percent}%",
                style = MaterialTheme.typography.bodySmall,
                color = TossGrey600,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(TossGrey100, RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(10.dp)
                    .background(graphColor, RoundedCornerShape(999.dp)),
            )
        }
    }
}

private fun categoryGraphColor(category: String?): Color {
    return when (category?.uppercase(Locale.US)) {
        "FOOD" -> TossBlue500
        "CAFE" -> TossOrange700
        "SHOPPING" -> TossTeal600
        "CULTURE" -> TossPurple600
        "TRANSPORT" -> TossGreen700
        "LODGING" -> Color(0xFF8B95A1)
        else -> TossGrey700
    }
}

@Composable
private fun CategoryChip(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StatusRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .background(
                    color = if (enabled) Color(0xFFD9F8E3) else Color(0xFFFFE0E0),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (enabled) "활성" else "비활성",
                color = if (enabled) Color(0xFF197A43) else Color(0xFFB42318),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("아직 수신된 알림이 없습니다.", fontWeight = FontWeight.Bold)
            Text(
                "1. Triplet에서 여행모드를 켭니다.\n2. 알림 접근을 허용합니다.\n3. Test Notifier 앱에서 결제 알림을 보냅니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyExpenseCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("아직 소비 내역이 없습니다.", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExpenseCard(expense: MatchedExpense) {
    val amountFormat = remember { NumberFormat.getIntegerInstance(Locale.KOREA) }
    val isDirectLocation =
        expense.txId.startsWith("manual-") &&
            expense.latitude != null &&
            expense.longitude != null &&
            expense.accuracyM == null
    val matchLabel =
        when {
            isDirectLocation -> "직접 선택"
            expense.reviewStatus == MatchReviewStatus.AUTO_CONFIRMED -> "자동 매칭"
            else -> "확인 필요"
        }
    val matchContainerColor =
        when {
            isDirectLocation -> TossBlue50
            expense.reviewStatus == MatchReviewStatus.AUTO_CONFIRMED -> TossGreen50
            else -> TossOrange50
        }
    val matchContentColor =
        when {
            isDirectLocation -> TossBlue600
            expense.reviewStatus == MatchReviewStatus.AUTO_CONFIRMED -> TossGreen700
            else -> TossOrange700
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, TossGrey200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.merchantName,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = TripletTimeFormatter.format(expense.occurredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${amountFormat.format(expense.amountMinor)}원",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = TossGrey900,
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoTag(
                    label = displayCategory(expense.category),
                    containerColor = TossBlue50,
                    contentColor = TossBlue600,
                )
                InfoTag(
                    label = matchLabel,
                    containerColor = matchContainerColor,
                    contentColor = matchContentColor,
                )
            }

            if (!expense.note.isNullOrBlank()) {
                Text(
                    text = expense.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocationSampleCard(sample: LocationSample) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, TossGrey200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "lat=${String.format(Locale.US, "%.5f", sample.latitude)}, lng=${String.format(Locale.US, "%.5f", sample.longitude)}",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "수집 시각 ${TripletTimeFormatter.format(sample.capturedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "정확도 ${sample.accuracyM}m · ${sample.source}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationEventCard(event: NotificationDebugEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, TossGrey200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = event.stage.label,
                    fontWeight = FontWeight.Bold,
                    color = event.stage.color,
                )
                Text(
                    text = TripletTimeFormatter.format(event.recordedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = event.summary, fontWeight = FontWeight.SemiBold)
            Text(
                text = event.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (event.packageName.isNotBlank()) {
                Text(
                    text = "package: ${event.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun startTravelMode(context: Context) {
    TripletTravelStore.startNewTrip()
    TravelLocationService.start(context)
}

private fun stopTravelMode(context: Context) {
    TripletTravelStore.updateTravelModeEnabled(false)
    TravelLocationService.stop(context)
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false
    val componentName = ComponentName(context, com.triplet.app.notification.PaymentNotificationListenerService::class.java)
    return enabledListeners.contains(componentName.flattenToString())
}
