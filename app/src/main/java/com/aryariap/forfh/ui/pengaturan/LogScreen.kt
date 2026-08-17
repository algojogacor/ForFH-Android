package com.aryariap.forfh.ui.pengaturan

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.debug.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Jumlah baris terakhir yang ditampilkan (arsip + aktif, urutan kronologis). */
private const val MAX_LOG_LINES = 200

/**
 * Layar Log aplikasi: baris log terakhir + tombol "Bagikan log" (ACTION_SEND via
 * FileProvider, filesDir/logs) dan "Hapus log". Pola InfoScreen: Scaffold dengan
 * judul titleLarge (bukan TopAppBar — konsisten dengan layar lain), state jujur:
 * loading ("Memuat log..."), kosong (penyebab + kapan terisi), konten.
 *
 * Teks log memakai FontFamily.Monospace: baris log berbentuk tabel (waktu | level |
 * tag | pesan) dan monospace menjaga kolom sejajar untuk dipindai cepat — suara
 * tipografi yang sudah dipakai app (labelLarge di Type.kt), bukan sekadar gaya.
 */
@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf<List<String>?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    // Baca file di Dispatchers.IO — baca I/O tidak pernah di main thread.
    LaunchedEffect(refresh) {
        lines = withContext(Dispatchers.IO) { AppLog.readRecent(MAX_LOG_LINES) }
    }

    val hasLog = remember(lines) { lines?.isNotEmpty() == true }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Text("Log aplikasi", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { shareLog(context) },
                    enabled = hasLog,
                    modifier = Modifier.weight(1f),
                ) { Text("Bagikan log") }
                OutlinedButton(
                    onClick = {
                        AppLog.clear()
                        AppLog.info("LogScreen", "log dihapus oleh user")
                        refresh++
                    },
                    enabled = hasLog,
                    modifier = Modifier.weight(1f),
                ) { Text("Hapus log") }
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                val current = lines
                when {
                    current == null -> CenteredHint {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Memuat log...", style = MaterialTheme.typography.bodyMedium)
                    }
                    current.isEmpty() -> CenteredHint {
                        Text(
                            "Belum ada log. Alarm, sinkronisasi, dan notifikasi akan tercatat di sini otomatis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> Card(modifier = Modifier.fillMaxSize()) {
                        // Teks dipilih → user bisa menyalin baris tertentu tanpa bagikan file.
                        SelectionContainer {
                            Text(
                                text = current.joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredHint(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/** Bagikan file log aktif via ACTION_SEND (FileProvider). Kegagalan hanya di-log. */
private fun shareLog(context: Context) {
    val file = AppLog.activeFile() ?: return
    if (!file.exists() || file.length() == 0L) return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Log ForFH ${BuildConfig.VERSION_NAME}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Bagikan log"))
    } catch (t: Throwable) {
        AppLog.error("LogScreen", "bagikan log gagal: ${t.message}")
    }
}
