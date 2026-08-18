package com.aryariap.forfh.ui.pengaturan

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.data.prefs.AlarmOffsets
import com.aryariap.forfh.data.prefs.formatOffsetMinutes
import com.aryariap.forfh.ui.UiFormat
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
    if (showLog) {
        BackHandler { showLog = false }
        LogScreen(onBack = { showLog = false })
        return
    }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
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
                            text = "Semua alarm kuliah hari ini dimatikan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                        TonalButton(
                            text = "Aktifkan Lagi Alarm Hari Ini",
                            onClick = { viewModel.unmuteToday() },
                            height = 44.dp,
                        )
                    } else {
                        OutlineButton(
                            text = "Senyapkan Alarm Hari Ini",
                            onClick = { viewModel.muteToday() },
                            height = 44.dp,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    var dialogDay by rememberSaveable { mutableStateOf<Int?>(null) }
                    for ((day, label) in DAY_ORDER) {
                        DayOffsetsSection(
                            day = day,
                            label = label,
                            offsets = state.offsets.offsetsFor(day),
                            onAdd = { dialogDay = day },
                            onRemove = { minutes -> viewModel.removeOffset(day, minutes) },
                        )
                    }

                    if (dialogDay != null) {
                        AddOffsetDialog(
                            dayLabel = DAY_ORDER.first { it.first == dialogDay }.second,
                            existing = state.offsets.offsetsFor(dialogDay!!),
                            onConfirm = { minutes ->
                                viewModel.addOffset(dialogDay!!, minutes)
                                dialogDay = null
                            },
                            onDismiss = { dialogDay = null },
                        )
                    }
                }
            }

            // Group 2: Sinkronisasi
            ForfhSectionLabel("Sinkronisasi Data")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Terakhir sinkron: ${UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)}",
                        style = ForfhTypeExtras.MonoMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton(
                        text = "Sinkronkan Sekarang",
                        onClick = { viewModel.syncNow() },
                        height = 46.dp,
                    )
                }
            }

            // Group 3: Izin Perangkat
            ForfhSectionLabel("Izin Perangkat")
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        OutlineButton(
                            text = "Izin Notifikasi",
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            },
                            height = 44.dp,
                        )
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        OutlineButton(
                            text = "Alarm Presisi",
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                )
                            },
                            height = 44.dp,
                        )
                    }
                    if (Build.VERSION.SDK_INT >= 34) {
                        OutlineButton(
                            text = "Alarm Layar Penuh",
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                )
                            },
                            height = 44.dp,
                        )
                    }
                    Text(
                        text = "Petunjuk MIUI/HyperOS: pastikan izin \"Buka di layar kunci\" aktif dan nonaktifkan penghemat baterai untuk ForFH.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

            // Group 5: Sesi & Keluar
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

            Text(
                text = "ForFH v${BuildConfig.VERSION_NAME}",
                style = ForfhTypeExtras.MonoMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
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
