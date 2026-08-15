package com.aryariap.forfh.ui.pengaturan

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.ui.UiFormat

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
        ToggleRow("3 jam sebelum", state.offsets.offset3h) { viewModel.setOffset(180, it) }
        ToggleRow("2 jam sebelum", state.offsets.offset2h) { viewModel.setOffset(120, it) }
        ToggleRow("1 jam sebelum", state.offsets.offset1h) { viewModel.setOffset(60, it) }

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

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
