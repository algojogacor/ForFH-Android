package com.aryariap.forfh.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhSectionLabel
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTopBar
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.PresensiRing
import com.aryariap.forfh.ui.theme.PrimaryButton

@Composable
fun InfoScreen(viewModel: InfoViewModel) {
    val state by viewModel.state.collectAsState()
    val hasData = state.presensi.isNotEmpty() || state.cards.isNotEmpty()

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            ForfhTopBar(
                title = "Info",
                eyebrow = "INFORMASI AKADEMIK",
                trailing = {
                    IconButton(onClick = viewModel::syncNow) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Sinkronkan",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.syncActivity == SyncActivity.QUEUED) {
                QueuedBanner()
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    !state.loaded -> FullState {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Memuat data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.syncActivity == SyncActivity.RUNNING && !hasData -> FullState {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Menyinkronkan data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.connected == false -> FullState {
                        ForfhSurface {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Kampus Belum Terhubung",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Akun Kampus Kita belum terhubung di ForFH web. Hubungkan di web, lalu sinkronkan lagi.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                state.kampusLastSyncAt?.let { iso ->
                                    InfoFormat.formatUpdatedAt(iso)?.let { last ->
                                        Text(
                                            text = "Terakhir sinkron kampus: $last",
                                            style = ForfhTypeExtras.MonoMeta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                PrimaryButton(
                                    text = "Sinkronkan",
                                    onClick = viewModel::syncNow,
                                    height = 46.dp,
                                )
                            }
                        }
                    }

                    state.error && !hasData -> FullState {
                        ForfhSurface {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Sinkronisasi Gagal",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = "Periksa koneksi internet lalu coba lagi.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                PrimaryButton(
                                    text = "Coba Lagi",
                                    onClick = viewModel::syncNow,
                                    height = 46.dp,
                                )
                            }
                        }
                    }

                    !hasData -> FullState {
                        ForfhSurface {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Belum Ada Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Sinkronkan untuk memuat data akademik kampus.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                PrimaryButton(
                                    text = "Sinkronkan",
                                    onClick = viewModel::syncNow,
                                    height = 46.dp,
                                )
                            }
                        }
                    }

                    else -> InfoContent(state = state, onSync = viewModel::syncNow)
                }
            }
        }
    }
}

@Composable
private fun QueuedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        Text(
            text = "Menunggu jaringan untuk sinkron...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FullState(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun InfoContent(state: InfoUiState, onSync: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.error) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Sinkronisasi gagal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onSync) { Text("Coba lagi") }
                }
            }
        }

        if (state.presensi.isNotEmpty()) {
            item {
                ForfhSectionLabel("Presensi Semester Ini")
            }
            items(state.presensi) { row ->
                PresensiCard(row)
            }
        }

        if (state.cards.isNotEmpty()) {
            items(state.cards, key = { it.jenis }) { card ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForfhSectionLabel(card.title)
                    InfoKampusCard(card)
                }
            }
        }

        item {
            Text(
                text = InfoFormat.kampusUpdatedText(state.kampusLastSyncAt),
                style = ForfhTypeExtras.MonoMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun PresensiCard(row: PresensiRow) {
    ForfhSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = row.kode,
                    style = ForfhTypeExtras.MonoMeta,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = row.nama,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = InfoFormat.formatPresensi(row.tm, row.hadir, row.persen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            row.persen?.let {
                PresensiRing(percentage = it)
            }
        }
    }
}
