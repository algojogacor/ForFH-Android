package com.aryariap.forfh.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.theme.ForfhColors

/**
 * Kartu info kampus per jenis (V1.1 Task 9) — render model berdesain per jenis, bukan
 * dump label:nilai. DNA yang dipakai: kartu surface + padding 14dp + kode mono primary
 * (labelLarge), nama titleMedium, baris sekunder onSurfaceVariant dengan pemisah "·",
 * badge status pola StatusChip (TugasListScreen), nilai fokus di kanan (pola persen%
 * PresensiCard). Dials app: ENERGY 1 / RHYTHM 2 / MOTION 1 — tidak ada emoji, tidak ada
 * em dash (R-02), semua angka dari Room (R-17).
 */
@Composable
fun InfoKampusCard(card: InfoCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (card.model.isEmpty) {
                Text(
                    text = "Tidak ada data untuk kategori ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                when (val model = card.model) {
                    is IdentityCard -> IdentityCardView(model)
                    is CourseListModel -> model.courses.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider()
                        CourseRowView(row)
                    }
                    is HerListModel -> model.rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider()
                        HerRowView(row)
                    }
                    is PaymentListModel -> model.rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider()
                        PaymentRowView(row)
                    }
                    is CalendarListModel -> model.rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider()
                        CalendarRowView(row)
                    }
                    is SummaryCardModel -> SummaryCardView(model)
                    is DosenWaliModel -> model.dosen.forEachIndexed { i, d ->
                        if (i > 0) HorizontalDivider()
                        DosenWaliView(d)
                    }
                    is InstructionBlockModel -> InstructionBlockView(model)
                    is GenericRowModel -> GenericRowsView(model.rows)
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

/**
 * Kartu identitas (status_mhs): NIM sebagai kode identitas (mono primary, pola kode MK),
 * badge status akademik di kanan (badge fungsional R-09 — nilai asli STATUS_AKADEMIK,
 * warna dari tone: Aktif = hijau Success, lain = netral, pola StatusChip), nama sebagai
 * judul, fakta sebagai grid 2 kolom. Status adalah satu-satunya informasi yang butuh
 * warna pada kartu ini — satu aksen, bukan warna di mana-mana.
 */
@Composable
private fun IdentityCardView(model: IdentityCard) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            model.nim?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            model.status?.let { StatusBadge(it) }
        }
        model.nama?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        val facts = buildList {
            model.prodi?.let { add("Prodi" to it) }
            model.jenjang?.let { add("Jenjang" to it) }
            model.fakultas?.let { add("Fakultas" to it) }
            model.angkatan?.let { add("Angkatan" to it) }
            model.jk?.let { add("Jenis Kelamin" to it) }
            model.agama?.let { add("Agama" to it) }
            addAll(model.extras)
        }
        if (facts.isNotEmpty()) {
            HorizontalDivider()
            FactGrid(facts)
        }
    }
}

/** Badge status — pola StatusChip TugasListScreen; teks SELALU nilai asli (R-38). */
@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (InfoCardModels.statusTone(status)) {
        StatusTone.POSITIVE -> ForfhColors.Success to Color.White
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = status,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * Baris peserta MK — DNA PresensiCard: kode mono primary di atas, nama titleMedium,
 * baris sekunder "Kelas · Dosen · Hari · Ruang", chip SKS di kanan (fungsi: kredit
 * kuliah; R-09), sisa field (kuota/peserta) sebagai baris kecil — data tidak dibuang.
 */
@Composable
private fun CourseRowView(row: CourseRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            row.kode?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            row.nama?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val secondary = buildList {
                row.kelas?.let { add("Kelas $it") }
                row.dosen?.let { add(it) }
                row.hari?.let { add(it) }
                row.ruang?.let { add(it) }
            }.joinToString(" · ")
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExtraRows(row.extras)
        }
        row.sks?.let {
            Text(
                text = "$it SKS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * Baris HER — nama MK sebagai judul, no ujian + periode + SKS sebagai baris sekunder,
 * nilai huruf sebagai angka inti di kanan (pola persen% PresensiCard — nilai adalah
 * informasi yang dicari user pada kartu ini).
 */
@Composable
private fun HerRowView(row: HerRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            row.noUjian?.let {
                Text(
                    text = "No. Ujian $it",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            row.nama?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val secondary = buildList {
                row.periode?.let { add(it) }
                row.sks?.let { add("$it SKS") }
            }.joinToString(" · ")
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExtraRows(row.extras)
        }
        row.grade?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/**
 * Baris pembayaran — kegiatan (atau semester) sebagai judul, nominal Rupiah sebagai
 * angka inti di kanan (jumlah adalah informasi yang dicari), tanggal bayar + status
 * sebagai baris sekunder. Nominal hanya dari data (R-17) dan diformat murni.
 */
@Composable
private fun PaymentRowView(row: PaymentRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            (row.kegiatan ?: row.semester)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val bayar = row.tglBayar?.let { InfoFormat.formatIsoDate(it) }?.let { "Dibayar $it" }
            val secondary = buildList {
                if (row.kegiatan != null) row.semester?.let { add(it) }
                bayar?.let { add(it) }
                row.status?.let { add(it) }
            }.joinToString(" · ")
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExtraRows(row.extras)
        }
        InfoFormat.formatRupiah(row.nominal)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** Baris kalender — kegiatan sebagai judul, rentang tanggal sebagai baris sekunder. */
@Composable
private fun CalendarRowView(row: CalendarRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        row.kegiatan?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val mulai = row.mulai?.let { InfoFormat.formatIsoDate(it) }
        val selesai = row.selesai?.let { InfoFormat.formatIsoDate(it) }
        val range = when {
            mulai != null && selesai != null -> if (mulai == selesai) mulai else "$mulai s/d $selesai"
            mulai != null -> mulai
            selesai != null -> selesai
            else -> null
        }
        range?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ExtraRows(row.extras)
    }
}

/**
 * Kartu rekap (masa_studi / sks_aktif / penyerahan_ktm): fakta kunci sebagai grid
 * stat 2 kolom (label kecil di atas nilai), sisa field sebagai baris label:nilai di
 * bawah — cepat dibaca, tidak ada data yang dibuang.
 */
@Composable
private fun SummaryCardView(model: SummaryCardModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (model.headline.isNotEmpty()) {
            FactGrid(model.headline)
        }
        if (model.headline.isNotEmpty() && model.rows.isNotEmpty()) {
            HorizontalDivider()
        }
        model.rows.forEach { (label, value) -> FactRow(label, value) }
    }
}

/** Dosen wali — kartu ringkas: nama sebagai judul, fakta lain sebagai baris berlabel. */
@Composable
private fun DosenWaliView(d: DosenFacts) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        d.nama?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (d.facts.isNotEmpty()) {
            HorizontalDivider()
            d.facts.forEach { (label, value) -> FactRow(label, value) }
        }
    }
}

/**
 * Instruksi tugas — blok teks terbaca: kode kursus (mono primary) + nama, lalu per
 * section: nama section (redup) + teks instruksi apa adanya (web sudah membersihkan
 * HTML) + daftar aktivitas (nama tugas di section, "·" sebagai penanda baris — motif
 * pemisah app). Prosa dirender sebagai prosa, bukan baris label:nilai.
 */
@Composable
private fun InstructionBlockView(model: InstructionBlockModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        model.courses.forEachIndexed { i, course ->
            if (i > 0) HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                course.kode?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                course.nama?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                course.sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        section.nama?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        section.teks?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        section.assignments.forEach { assignment ->
                            Text(
                                text = "· $assignment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Fallback jenis tak dikenal — blok label:nilai dengan label map penuh (humanize). */
@Composable
private fun GenericRowsView(rows: InfoRows) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.blocks.forEachIndexed { i, block ->
            if (i > 0) HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.rows.forEach { (label, value) ->
                    FactRow(label, value)
                }
            }
        }
        if (rows.skippedRecords > 0) {
            Text(
                text = "+${rows.skippedRecords} lainnya",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Grid fakta 2 kolom: label kecil di atas nilai — kompak, mudah dipindai. */
@Composable
private fun FactGrid(facts: List<Pair<String, String>>) {
    facts.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    FactRow(label, value)
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** Baris fakta: label kecil redup di atas nilai. */
@Composable
private fun FactRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Sisa field baris typed (kuota, peserta, dll.) — baris kecil, label map penuh. */
@Composable
private fun ExtraRows(extras: List<Pair<String, String>>) {
    if (extras.isEmpty()) return
    Text(
        text = extras.joinToString(" · ") { (label, value) -> "$label $value" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
