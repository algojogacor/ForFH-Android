package com.aryariap.forfh.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.UiFormat

/**
 * Layar Info (V1.1 Task 8): rekap presensi per MK (hadir/tm/persen — web tidak punya
 * izin/sakit/alpa, ruling R22) + kartu info kampus per jenis (dataJson mentah → label
 * Indonesia). Semua angka dari Room (R-17); state jujur per R-27: loading saat sync
 * berjalan TANPA data, error saat sync gagal TANPA data, terputus saat connected=false,
 * kosong saat belum pernah sync; dengan data lama tetap → konten + baris error.
 *
 * Preferensi tampilan: saat data lama masih ada, data TETAP ditampilkan selama sync
 * berjalan/gagal (data lama yang nyata lebih berguna daripada layar kosong; Room flow
 * meng-update konten otomatis saat sync selesai). Spinner hanya saat tidak ada apa pun
 * untuk ditampilkan. Tiap state penuh punya TEPAT SATU tombol aksi (R-26).
 */
@Composable
fun InfoScreen(viewModel: InfoViewModel) {
    val state by viewModel.state.collectAsState()
    val hasData = state.presensi.isNotEmpty() || state.cards.isNotEmpty()

    Scaffold(
        topBar = {
            Text(
                text = "Info",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    ) { padding ->
        when {
            state.loading && !hasData -> FullState(padding) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Menyinkronkan data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Putus lebih fundamental daripada error transient: retry sync tidak akan
            // memperbaiki akun yang tidak terhubung — tunjukkan penyebab + aksinya.
            state.connected == false -> FullState(padding) {
                Text(
                    text = "Kampus belum terhubung.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Akun Kampus Kita belum terhubung di ForFH web. Hubungkan di web, lalu sinkronkan lagi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.kampusLastSyncAt?.let { iso ->
                    InfoFormat.formatUpdatedAt(iso)?.let { last ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Terakhir sinkron kampus: $last",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::syncNow) { Text("Sinkronkan") }
            }

            state.error && !hasData -> FullState(padding) {
                Text(
                    text = "Sinkronisasi gagal. Cek koneksi, lalu coba lagi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::syncNow) { Text("Coba lagi") }
            }

            !hasData -> FullState(padding) {
                Text(
                    text = "Belum ada data. Sinkronkan dulu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::syncNow) { Text("Sinkronkan") }
            }

            else -> InfoContent(state = state, onSync = viewModel::syncNow, padding = padding)
        }
    }
}

/** Wrapper state penuh (loading/error/putus/kosong): konten di tengah layar. */
@Composable
private fun FullState(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun InfoContent(state: InfoUiState, onSync: () -> Unit, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Sync gagal dengan data lama: data tetap tampil + baris error yang actionable.
        if (state.error) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            item { SectionHeader("Rekap presensi") }
            items(state.presensi) { row -> PresensiCard(row) }
        }

        if (state.cards.isNotEmpty()) {
            item { SectionHeader("Info kampus") }
            items(state.cards) { card -> InfoKampusCard(card) }
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/**
 * Kartu rekap presensi per MK: kode (mono, primary) + nama + baris hadir, dengan persen
 * sebagai angka inti di kanan (R-17: dari Room; hanya saat server mengirimnya). Variasi
 * dari kartu info kampus (R-14): satu angka focal di kanan, bukan kolom label:nilai.
 */
@Composable
private fun PresensiCard(row: PresensiRow) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = row.kode,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = row.nama,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = InfoFormat.formatPresensi(row.tm, row.hadir, row.persen),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (row.persen != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${row.persen}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Kartu info kampus per jenis: judul jenis + baris label:nilai per record (dataJson mentah
 * UPPERCASE_SNAKE → label Indonesia via InfoFormat). Nilai null/"" dilewati; record dipangkas
 * setelah batas tampil dengan catatan "+N lainnya" (jujur, pola web). Footer "Sinkron" hanya
 * saat updatedAt bisa diparse.
 */
@Composable
private fun InfoKampusCard(card: InfoCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            if (card.rows.blocks.isEmpty()) {
                Text(
                    text = "Tidak ada data untuk kategori ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                card.rows.blocks.forEachIndexed { i, block ->
                    if (i > 0) HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.rows.forEach { (label, value) ->
                            Row {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(0.42f),
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(0.58f),
                                )
                            }
                        }
                    }
                }
                if (card.rows.skippedRecords > 0) {
                    Text(
                        text = "+${card.rows.skippedRecords} lainnya",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            card.updatedAt?.let { ua ->
                Text(
                    text = "Sinkron $ua",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
