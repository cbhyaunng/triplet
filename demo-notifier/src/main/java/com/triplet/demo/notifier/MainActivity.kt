package com.triplet.demo.notifier

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoNotifierApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoNotifierApp() {
    val context = LocalContext.current
    val history = remember { mutableStateListOf<DemoPaymentPayload>() }
    var merchant by rememberSaveable { mutableStateOf("스타벅스") }
    var amountText by rememberSaveable { mutableStateOf("12800") }
    var currency by rememberSaveable { mutableStateOf("KRW") }
    var category by rememberSaveable { mutableStateOf(DemoCategory.CAFE.code) }
    var note by rememberSaveable { mutableStateOf("오사카역 지점") }
    var statusMessage by rememberSaveable { mutableStateOf("알림을 보내 Triplet listener를 테스트하세요.") }

    val requestNotificationsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        statusMessage = if (granted) {
            "알림 권한이 허용되었습니다."
        } else {
            "알림 권한이 거절되어 발송할 수 없습니다."
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Triplet Test Notifier")
                            Text(
                                text = "발표용 결제 알림 발송 앱",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                state = rememberLazyListState(),
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("빠른 템플릿", fontWeight = FontWeight.Bold)
                            TemplateButtonRow(label = "스타벅스 12,800원") {
                                merchant = "스타벅스"
                                amountText = "12800"
                                category = DemoCategory.CAFE.code
                                note = "오사카역 지점"
                            }
                            TemplateButtonRow(label = "세븐일레븐 5,000원") {
                                merchant = "세븐일레븐"
                                amountText = "5000"
                                category = DemoCategory.SHOPPING.code
                                note = "편의점 간식"
                            }
                            TemplateButtonRow(label = "공항철도 4,450원") {
                                merchant = "공항철도"
                                amountText = "4450"
                                category = DemoCategory.TRANSPORT.code
                                note = "교통비"
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("결제 알림 입력", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = merchant,
                                onValueChange = { merchant = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("가맹점명") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it.filter(Char::isDigit) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("금액 (amount_minor)") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = currency,
                                onValueChange = { currency = it.uppercase() },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("통화 코드") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it.uppercase() },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("카테고리") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("메모") },
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val amountMinor = amountText.toLongOrNull()
                                        if (merchant.isBlank() || amountMinor == null || amountMinor <= 0L) {
                                            statusMessage = "가맹점명과 올바른 금액을 입력해 주세요."
                                            return@Button
                                        }

                                        if (!canPostNotifications(context)) {
                                            requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@Button
                                        }

                                        val payload = DemoPaymentPayload(
                                            txId = DemoTxIdGenerator.next(),
                                            merchantName = merchant.trim(),
                                            amountMinor = amountMinor,
                                            currencyCode = currency.ifBlank { "KRW" }.uppercase(),
                                            category = category.ifBlank { DemoCategory.ETC.code }.uppercase(),
                                            occurredAt = OffsetDateTime.now(),
                                            note = note.trim().ifBlank { null },
                                        )

                                        DemoNotificationSender.send(context, payload)
                                        history.add(0, payload)
                                        statusMessage = "알림 발송 완료: ${payload.merchantName} / ${payload.amountMinor} ${payload.currencyCode}"
                                    },
                                ) {
                                    Text("지금 발송")
                                }
                                OutlinedButton(
                                    onClick = {
                                        merchant = ""
                                        amountText = ""
                                        currency = "KRW"
                                        category = DemoCategory.CAFE.code
                                        note = ""
                                        statusMessage = "입력값을 초기화했습니다."
                                    },
                                ) {
                                    Text("초기화")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("상태", fontWeight = FontWeight.Bold)
                            Text(
                                text = statusMessage,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "패키지명: com.triplet.demo.notifier",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "채널 ID: ${DemoNotificationSender.channelId}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                item {
                    Text("최근 발송 이력", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                if (history.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "아직 발송한 알림이 없습니다.",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(history, key = { it.txId }) { payload ->
                        HistoryCard(payload)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateButtonRow(
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick) {
        Text(label)
    }
}

@Composable
private fun HistoryCard(payload: DemoPaymentPayload) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${payload.merchantName} / ${payload.amountMinor} ${payload.currencyCode}",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "tx_id=${payload.txId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "occurred_at=${formatter.format(payload.occurredAt.toInstant())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            payload.note?.let {
                Divider()
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun canPostNotifications(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private object DemoTxIdGenerator {
    fun next(): String {
        return "demo-${OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}-${UUID.randomUUID().toString().take(6)}"
    }
}
