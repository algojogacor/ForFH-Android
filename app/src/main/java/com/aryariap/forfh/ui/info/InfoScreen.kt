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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * Layar Info (V1.1 Task 8+9): rekap presensi per MK (hadir/tm/persen — web tidak punya
 * izin/sakit/alpa, ruling R22) + kartu info kampus per jenis (section header Indonesia +
 * kartu berdesain per jenis: identitas, daftar MK, HER, pembayaran, dst. — bukan dump
 * mentah). Semua angka dari Room (R-17); state jujur per R-27:
 *
 * - QUEUED (worker menunggu jaringan) → banner kecil, BUKAN spinner layar penuh yang bisa
 *   tampil tanpa batas; state di bawahnya (kosong/putus/error/konten) tetap terlihat
 *   lengkap dengan aksinya (fix review).
 * - RUNNING tanpa data → spinner layar penuh "Menyinkronkan data..."; dengan data →
 *   konten tetap tampil (Room flow meng-update otomatis saat selesai).
 * - Putus (connected=false) lebih fundamental daripada error transient: retry sync tidak
 *   memperbaiki akun yang tidak terhubung — tunjukkan penyebab + aksinya.
 * - Frame pertama (Room belum ter-emisi) → indikator "Memuat data...", bukan asumsi state
 *   terminal (fix review: guard loaded).
 *
 * Tiap state penuh punya TEPAT SATU tombol aksi (R-26); tidak ada state tanpa jalan keluar.
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Worker menunggu jaringan (ENQUEUED): banner kecil non-blocking — state utama
            // di bawahnya tetap terlihat, tidak ada spinner tanpa batas.
            if (state.syncActivity == SyncActivity.QUEUED) {
                QueuedBanner()
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    !state.loaded -> FullState {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Memuat data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.syncActivity == SyncActivity.RUNNING && !hasData -> FullState {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Menyinkronkan data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.connected == false -> FullState {
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

                    state.error && !hasData -> FullState {
                        Text(
                            text = "Sinkronisasi gagal. Cek koneksi, lalu coba lagi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = viewModel::syncNow) { Text("Coba lagi") }
                    }

                    !hasData -> FullState {
                        Text(
                            text = "Belum ada data. Sinkronkan dulu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = viewModel::syncNow) { Text("Sinkronkan") }
                    }

                    else -> InfoContent(state = state, onSync = viewModel::syncNow)
                }
            }
        }
    }
}

/** Banner kecil: worker sync menunggu jaringan — jujur, bukan klaim "sedang sinkron". */
@Composable
private fun QueuedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Menunggu jaringan untuk sinkron...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f), // melipat, bukan overflow, di layar sempit (R-03)
        )
    }
}

/** Wrapper state penuh (loading/error/putus/kosong): konten di tengah layar. */
@Composable
private fun FullState(
    content: @Composable () -> Unit,
) {
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

        // Satu section header per jenis (keluhan user: dump mentah tanpa judul section) —
        // judul Indonesia per jenis, kartu berdesain per jenis di bawahnya.
        if (state.cards.isNotEmpty()) {
            items(state.cards, key = { it.jenis }) { card ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(card.title)
                    InfoKampusCard(card)
                }
            }
        }

        // Footer umur data kampus (meta.lastSyncAt) — bukan waktu sync jadwal/tugas, dan
        // tanpa em dash (fix review; UiFormat.syncInfo dibiarkan untuk layar lain).
        item {
            Text(
                text = InfoFormat.kampusUpdatedText(state.kampusLastSyncAt),
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

// Kartu info kampus per jenis (kartu identitas, daftar MK, HER, dst.) pindah ke
// InfoCardViews.kt — InfoKampusCard dipakai dari sana.
