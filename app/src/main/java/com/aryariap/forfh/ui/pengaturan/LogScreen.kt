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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.debug.AppLog
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.OutlineButton
import com.aryariap.forfh.ui.theme.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_LOG_LINES = 200

@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf<List<String>?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        lines = withContext(Dispatchers.IO) { AppLog.readRecent(MAX_LOG_LINES) }
    }

    val hasLog = remember(lines) { lines?.isNotEmpty() == true }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Log Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryButton(
                    text = "Bagikan Log",
                    onClick = { shareLog(context) },
                    enabled = hasLog,
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                )
                OutlineButton(
                    text = "Hapus Log",
                    onClick = {
                        AppLog.clear()
                        AppLog.info("LogScreen", "log dihapus oleh user")
                        refresh++
                    },
                    enabled = hasLog,
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val current = lines
                when {
                    current == null -> CenteredHint {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Memuat log...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    current.isEmpty() -> CenteredHint {
                        Text(
                            text = "Belum ada log. Alarm, sinkronisasi, dan notifikasi akan tercatat otomatis di sini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> ForfhSurface(modifier = Modifier.fillMaxSize()) {
                        SelectionContainer {
                            Text(
                                text = current.joinToString("\n"),
                                style = ForfhTypeExtras.MonoLog,
                                color = MaterialTheme.colorScheme.onSurface,
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

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
