package com.aryariap.forfh.ui.pengaturan

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.data.prefs.AlarmOffsets
import com.aryariap.forfh.data.prefs.formatOffsetMinutes
import com.aryariap.forfh.ui.UiFormat
import java.time.LocalDate

/** Urutan tampil Senin..Minggu; dayOfWeek konvensi API ForFH (0=Minggu). */
private val DAY_ORDER = listOf(
    1 to "Senin", 2 to "Selasa", 3 to "Rabu", 4 to "Kamis",
    5 to "Jumat", 6 to "Sabtu", 0 to "Minggu",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PengaturanScreen(viewModel: PengaturanViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.titleLarge)

        Text("Pengingat kuliah", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Alarm hanya untuk kuliah pertama hari itu — kamu masih di rumah dan mungkin tidur; " +
                "kuliah berikutnya (sudah di kampus) tanpa alarm. Setiap hari punya daftar menit bebas " +
                "sendiri (contoh: 90 = 1 j 30 m sebelum kelas).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // "Matikan seluruh alarm hari ini": sekali per hari, besok normal lagi
        // (tombol "Tutup" di layar alarm tetap hanya mematikan alarm yang berbunyi).
        val today = LocalDate.now().toString()
        val mutedToday = state.mutedDate == today
        if (mutedToday) {
            Text(
                "Semua alarm kuliah hari ini dimatikan — besok normal lagi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = { viewModel.unmuteToday() }, modifier = Modifier.fillMaxWidth()) {
                Text("Aktifkan lagi alarm hari ini")
            }
        } else {
            OutlinedButton(onClick = { viewModel.muteToday() }, modifier = Modifier.fillMaxWidth()) {
                Text("Matikan seluruh alarm hari ini")
            }
        }

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

        HorizontalDivider()

        Button(onClick = { viewModel.syncNow() }, modifier = Modifier.fillMaxWidth()) {
            Text("Sinkronkan sekarang")
        }
        Text(
            text = "Terakhir sinkron: ${UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text("Izin perangkat", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (Build.VERSION.SDK_INT >= 33) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, modifier = Modifier.fillMaxWidth()) { Text("Izin notifikasi") }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Alarm presisi (buka setelan)") }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Alarm layar penuh (buka setelan)") }
        }
        Text(
            text = "Petunjuk MIUI/HyperOS: jika alarm tidak berbunyi, buka Setelan > Aplikasi > ForFH, " +
                "aktifkan izin \"Alarm & pengingat\" dan \"Buka di layar kunci\", lalu nonaktifkan penghemat baterai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Keluar", color = MaterialTheme.colorScheme.error) }

        Text(
            text = "ForFH ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
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
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (offsets.isEmpty()) {
            Text(
                "Tidak ada alarm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (minutes in offsets) {
                    InputChip(
                        selected = false,
                        onClick = { onRemove(minutes) },
                        label = { Text(formatOffsetMinutes(minutes)) },
                        trailingIcon = {
                            IconButton(onClick = { onRemove(minutes) }, modifier = Modifier.height(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.height(16.dp))
                            }
                        },
                    )
                }
            }
        }
        TextButton(onClick = onAdd) { Text("+ Tambah") }
    }
}

/** Dialog input menit bebas (1..720) dengan preview jam-menit; duplikat & di luar batas ditolak. */
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
        title = { Text("Tambah alarm — $dayLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit).take(4) },
                    label = { Text("Menit sebelum kelas") },
                    supportingText = {
                        Text(
                            when {
                                duplicate -> "Sudah ada di daftar."
                                minutes == null -> "Angka ${AlarmOffsets.MIN_OFFSET_MINUTES}–${AlarmOffsets.MAX_OFFSET_MINUTES} (contoh: 90 = 1 j 30 m)."
                                else -> "→ ${formatOffsetMinutes(minutes)} sebelum kelas"
                            },
                        )
                    },
                    isError = minutes != null && (minutes !in AlarmOffsets.MIN_OFFSET_MINUTES..AlarmOffsets.MAX_OFFSET_MINUTES || duplicate),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes!!) }, enabled = valid) { Text("Tambah") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
