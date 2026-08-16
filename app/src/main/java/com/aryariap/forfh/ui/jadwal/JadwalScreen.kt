package com.aryariap.forfh.ui.jadwal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun JadwalScreen(viewModel: JadwalViewModel, nextUpViewModel: NextUpViewModel) {
    val state by viewModel.state.collectAsState()
    val nextUp by nextUpViewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    // Ticker kartu "Berikutnya": refresh 30 dtk HANYA di tab "Hari ini" (tab 0) — countdown
    // ada di tab itu, tab "Seminggu" tidak menampilkan waktu real-time jadi tak perlu refresh
    // berkala. Akurasi menit cukup untuk countdown, hemat baterai (bukan 1 dtk). Ticker di UI,
    // ViewModel hanya menyediakan refresh(). Berhenti otomatis saat layar keluar komposisi;
    // ganti tab → efek di-cancel & restart, jadi kembali ke tab 0 langsung refresh sekali.
    LaunchedEffect(tab) {
        if (tab != 0) return@LaunchedEffect
        while (isActive) {
            nextUpViewModel.refresh()
            delay(30_000)
        }
    }

    Scaffold(
        topBar = {
            Column {
                Text(
                    text = "Jadwal",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hari ini") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Seminggu") })
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tab == 0) {
                // Kartu "Berikutnya": di atas daftar hari ini, hanya saat ada data (kelas atau alarm).
                if (nextUp.nextClass != null || nextUp.nextAlarm != null) {
                    item {
                        NextUpCard(
                            state = nextUp,
                            onMute = nextUpViewModel::muteToday,
                            onUnmute = nextUpViewModel::unmuteToday,
                        )
                    }
                }
                items(state.today) { item -> KuliahCard(item) }
            } else {
                items(state.week) { hari ->
                    if (hari.items.isNotEmpty()) {
                        Text(
                            text = hari.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        hari.items.forEach { KuliahCard(it) }
                    }
                }
            }
            item {
                Text(
                    text = "Terakhir sinkron: ${UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Kartu "Berikutnya": kelas berikutnya + countdown (start WIB, ruling R5), alarm berikutnya,
 * dan quick mute "hari ini" (pola PengaturanScreen). Pemanggil hanya mengomposisikan kartu
 * saat ada data; baris di dalamnya opsional.
 */
@Composable
private fun NextUpCard(state: NextUpUiState, onMute: () -> Unit, onUnmute: () -> Unit) {
    val wib = ZoneId.of("Asia/Jakarta")
    val now = ZonedDateTime.now(wib)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(courseColor(state.nextClass?.first?.courseColor)),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.nextClass?.let { (kls, start) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            // "{nama}" = kode course (contoh plan: "PIH"), fallback nama lengkap.
                            text = "Kelas berikutnya: ${kls.courseCode ?: kls.courseName} (${UiFormat.timeOf(start)})",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        // Countdown tidak pernah terpotong: bagian nama yang panjang di-ellipsis,
                        // bagian "dalam ..." selalu utuh (inilah informasi inti kartu).
                        Text(
                            text = " · dalam ${UiFormat.countdownTo(now, start)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                state.nextAlarm?.let { alarm ->
                    Text(
                        text = "Alarm berikutnya ${UiFormat.timeOf(alarm.triggerAtMillis, wib)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Tombol mute kecil (pola PengaturanScreen, teks sama persis): "Aktifkan lagi
                // alarm hari ini" saat sedang mute; "Matikan seluruh alarm hari ini" hanya saat
                // ada alarm untuk dimatikan (tanpa alarm, tombol mute adalah kontrol mati).
                if (state.mutedToday) {
                    TextButton(onClick = onUnmute) { Text("Aktifkan lagi alarm hari ini") }
                } else if (state.nextAlarm != null) {
                    TextButton(onClick = onMute) { Text("Matikan seluruh alarm hari ini") }
                }
            }
        }
    }
}

/** Warna course dari hex "RRGGBB"; fallback accent bila tidak valid (pola KuliahCard). */
private fun courseColor(hex: String?): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(ForfhColors.Accent)

@Composable
private fun KuliahCard(item: JadwalItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(courseColor(item.color)),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(UiFormat.range(item.startTime, item.endTime))
                        when {
                            item.onlineUrl != null -> append(" · Daring")
                            !item.room.isNullOrBlank() -> append(" · ${item.room}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.courseCode?.let { code ->
                    Text(
                        text = code,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
