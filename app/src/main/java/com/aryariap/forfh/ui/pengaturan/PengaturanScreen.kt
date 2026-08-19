package com.aryariap.forfh.ui.pengaturan

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.data.changelog.ChangelogCatalog
import com.aryariap.forfh.data.prefs.AlarmOffsets
import com.aryariap.forfh.data.prefs.formatOffsetMinutes
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.changelog.ChangelogScreen
import com.aryariap.forfh.ui.changelog.WhatsNewBottomSheet
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhSectionLabel
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTopBar
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.OutlineButton
import com.aryariap.forfh.ui.theme.PrimaryButton
import com.aryariap.forfh.ui.theme.TonalButton
import java.time.LocalDate

private val DAY_ORDER = listOf(
    1 to "Senin", 2 to "Selasa", 3 to "Rabu", 4 to "Kamis",
    5 to "Jumat", 6 to "Sabtu", 0 to "Minggu",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PengaturanScreen(viewModel: PengaturanViewModel) {
    var showLog by rememberSaveable { mutableStateOf(false) }
    var showChangelog by rememberSaveable { mutableStateOf(false) }
    var showWhatsNewPreview by rememberSaveable { mutableStateOf(false) }

    if (showLog) {
        BackHandler { showLog = false }
        LogScreen(onBack = { showLog = false })
        return
    }

    if (showChangelog) {
        BackHandler { showChangelog = false }
        ChangelogScreen(onBack = { showChangelog = false })
        return
    }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    if (showWhatsNewPreview) {
        val latestEntry = ChangelogCatalog.getLatest(context)
        WhatsNewBottomSheet(
            entry = latestEntry,
            onDismiss = { showWhatsNewPreview = false },
            onViewAllHistory = {
                showWhatsNewPreview = false
                showChangelog = true
            },
        )
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = ForfhColors.PitchBlack,
        topBar = {
            ForfhTopBar(
                title = "Atur",
                eyebrow = "PREFERENSI & PERANGKAT",
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Group 1: Pengingat Kuliah
            ForfhSectionLabel("Pengingat Kuliah")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Alarm berbunyi sebelum kuliah pertama setiap hari. Tentukan menit pengingat untuk tiap hari.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val today = LocalDate.now().toString()
                    val mutedToday = state.mutedDate == today
                    if (mutedToday) {
                        Text(
                            text = "Seluruh alarm kuliah hari ini sedang dimatikan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlineButton(
                            text = "Nyalakan Kembali Alarm Hari Ini",
                            onClick = { viewModel.unmuteToday() },
                            height = 44.dp,
                        )
                    } else {
                        OutlineButton(
                            text = "Matikan Alarm Khusus Hari Ini",
                            onClick = { viewModel.muteToday() },
                            height = 44.dp,
                        )
                    }
                }
            }

            // Group 2: Jadwal Alarm per Hari
            ForfhSectionLabel("Jadwal Alarm per Hari")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    var editingDay by rememberSaveable { mutableStateOf<Pair<Int, String>?>(null) }

                    for ((day, label) in DAY_ORDER) {
                        val offsets = state.offsets.perDay[day].orEmpty()
                        DayOffsetsSection(
                            day = day,
                            label = label,
                            offsets = offsets,
                            onAdd = { editingDay = day to label },
                            onRemove = { minutes -> viewModel.removeOffset(day, minutes) },
                        )
                        if (day != DAY_ORDER.last().first) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }

                    editingDay?.let { (day, label) ->
                        AddOffsetDialog(
                            dayLabel = label,
                            existing = state.offsets.perDay[day].orEmpty(),
                            onConfirm = { minutes ->
                                viewModel.addOffset(day, minutes)
                                editingDay = null
                            },
                            onDismiss = { editingDay = null },
                        )
                    }
                }
            }

            // Group 3: Izin Sistem & Sinkronisasi
            ForfhSectionLabel("Izin & Sinkronisasi")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Izin Alarm Presisi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (Build.VERSION.SDK_INT >= 31) "Android 12+: perlu izin SCHEDULE_EXACT_ALARM" else "Tidak diperlukan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (Build.VERSION.SDK_INT >= 31) {
                            OutlineButton(
                                text = "Buka Setelan",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                height = 36.dp,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Sinkronisasi Data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val syncText = UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlineButton(
                            text = "Sinkron",
                            onClick = { viewModel.syncNow() },
                            height = 36.dp,
                        )
                    }
                }
            }

            // Group 4: Bantuan & Log
            ForfhSectionLabel("Bantuan & Diagnostik")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlineButton(
                        text = "Buka Log Aplikasi",
                        onClick = { showLog = true },
                        height = 44.dp,
                    )
                    Text(
                        text = "Catatan diagnostik aktivitas alarm, notifikasi, dan sinkronisasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Group 5: Tentang Aplikasi & Pembaruan
            ForfhSectionLabel("Tentang Aplikasi & Pembaruan")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // App Version Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "ForFH Android",
                                style = MaterialTheme.typography.titleMedium,
                                color = ForfhColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Versi ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = ForfhTypeExtras.MonoMeta,
                                color = ForfhColors.TextMuted,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ForfhColors.LinearIndigoSubtle,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForfhColors.LinearIndigo.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = ForfhTypeExtras.MonoMeta,
                                color = ForfhColors.LinearIndigo,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    HorizontalDivider(color = ForfhColors.BorderSubtle)

                    // Update Status Banner / Row
                    val updateInfo = state.updateInfo
                    if (updateInfo != null && updateInfo.hasUpdate) {
                        var expandedNotes by rememberSaveable { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ForfhColors.StatusProsesBg)
                                .border(1.dp, ForfhColors.StatusProsesFg.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Update Tersedia: v${updateInfo.latestVersion}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ForfhColors.StatusProsesFg,
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ForfhColors.StatusProsesFg.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = "BARU",
                                        style = ForfhTypeExtras.MonoMeta,
                                        color = ForfhColors.StatusProsesFg,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }

                            updateInfo.releaseTitle?.let { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ForfhColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            // Highlights from GitHub Release Notes
                            val highlights = updateInfo.releaseHighlights
                            if (highlights.isNotEmpty()) {
                                val displayList = if (expandedNotes || highlights.size <= 3) highlights else highlights.take(3)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ForfhColors.SurfaceSecondary)
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "Apa yang baru:",
                                        style = ForfhTypeExtras.MonoMeta,
                                        color = ForfhColors.TextMuted,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    for (item in displayList) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            androidx.compose.foundation.layout.Box(
                                                modifier = Modifier
                                                    .padding(top = 6.dp)
                                                    .size(4.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(ForfhColors.StatusProsesFg),
                                            )
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ForfhColors.TextSecondary,
                                                lineHeight = 16.sp,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                    if (highlights.size > 3) {
                                        Text(
                                            text = if (expandedNotes) "Sembunyikan" else "Lihat ${highlights.size - 3} poin lainnya...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForfhColors.LinearIndigo,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { expandedNotes = !expandedNotes }
                                                .padding(top = 2.dp),
                                        )
                                    }
                                }
                            }

                            PrimaryButton(
                                text = "Unduh Pembaruan",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                                    context.startActivity(intent)
                                },
                                height = 40.dp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                            ) {
                                Text(
                                    text = "Status Pembaruan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ForfhColors.TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = if (state.isCheckingUpdate) "Memeriksa versi terbaru..."
                                    else if (state.updateChecked) "Aplikasi sudah versi terbaru"
                                    else "Belum diperiksa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.updateChecked && updateInfo != null && !updateInfo.hasUpdate) ForfhColors.StatusSelesaiFg else ForfhColors.TextMuted,
                                )
                            }

                            OutlineButton(
                                text = if (state.isCheckingUpdate) "Memeriksa..." else "Cek Update",
                                onClick = { viewModel.checkForUpdates() },
                                modifier = Modifier.wrapContentWidth(),
                                height = 36.dp,
                                enabled = !state.isCheckingUpdate,
                            )
                        }
                    }

                    HorizontalDivider(color = ForfhColors.BorderSubtle)

                    // Changelog Button
                    OutlineButton(
                        text = "Lihat Catatan Perubahan (Changelog)",
                        onClick = { showChangelog = true },
                        height = 42.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Debug/Testing Helper for "Ada yang baru!"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TonalButton(
                            text = "Preview 'Ada yang Baru!'",
                            onClick = { showWhatsNewPreview = true },
                            height = 36.dp,
                            modifier = Modifier.weight(1f),
                        )
                        TonalButton(
                            text = "Reset Status Versi",
                            onClick = { viewModel.resetLastSeenVersion() },
                            height = 36.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Group 6: Sesi & Keluar
            ForfhSectionLabel("Sesi")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Keluar dari Akun",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.logout() }
                            .padding(vertical = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOffsetsSection(
    day: Int,
    label: String,
    offsets: List<Int>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "+ Tambah",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onAdd() }
                    .padding(4.dp),
            )
        }

        if (offsets.isEmpty()) {
            Text(
                text = "Tidak ada alarm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (minutes in offsets) {
                    InputChip(
                        selected = false,
                        onClick = { onRemove(minutes) },
                        label = {
                            Text(
                                text = formatOffsetMinutes(minutes),
                                style = ForfhTypeExtras.MonoMeta,
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { onRemove(minutes) }, modifier = Modifier.height(18.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    modifier = Modifier.height(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddOffsetDialog(
    dayLabel: String,
    existing: List<Int>,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val minutes = input.toIntOrNull()
    val duplicate = minutes != null && minutes in existing
    val valid = minutes != null && minutes in AlarmOffsets.MIN_OFFSET_MINUTES..AlarmOffsets.MAX_OFFSET_MINUTES && !duplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Alarm · $dayLabel",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit).take(4) },
                    label = { Text("Menit sebelum kelas") },
                    supportingText = {
                        Text(
                            text = when {
                                duplicate -> "Sudah ada di daftar."
                                minutes == null -> "Angka ${AlarmOffsets.MIN_OFFSET_MINUTES}–${AlarmOffsets.MAX_OFFSET_MINUTES} (contoh: 90 = 1 j 30 m)."
                                else -> "→ ${formatOffsetMinutes(minutes)} sebelum kelas"
                            },
                        )
                    },
                    isError = minutes != null && (minutes !in AlarmOffsets.MIN_OFFSET_MINUTES..AlarmOffsets.MAX_OFFSET_MINUTES || duplicate),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes!!) }, enabled = valid) {
                Text("Tambah", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
