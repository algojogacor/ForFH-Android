package com.aryariap.forfh.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhStatusPill
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras

@Composable
fun InfoKampusCard(card: InfoCard) {
    ForfhSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        CourseRowView(row)
                    }
                    is HerListModel -> model.rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        HerRowView(row)
                    }
                    is PaymentListModel -> model.rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        PaymentRowView(row)
                    }
                    is CalendarListModel -> model.rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        CalendarRowView(row)
                    }
                    is SummaryCardModel -> SummaryCardView(model)
                    is DosenWaliModel -> model.dosen.forEachIndexed { i, d ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        DosenWaliView(d)
                    }
                    is InstructionBlockModel -> InstructionBlockView(model)
                    is GenericRowModel -> GenericRowsView(model.rows)
                }
            }
            card.updatedAt?.let { ua ->
                Text(
                    text = "Sinkron $ua",
                    style = ForfhTypeExtras.MonoMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IdentityCardView(model: IdentityCard) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            model.nim?.let {
                Text(
                    text = it,
                    style = ForfhTypeExtras.MonoMeta,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            FactGrid(facts)
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (InfoCardModels.statusTone(status)) {
        StatusTone.POSITIVE -> ForfhColors.StatusSelesaiBg to ForfhColors.StatusSelesaiFg
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    ForfhStatusPill(text = status, foreground = fg, background = bg)
}

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
                    style = ForfhTypeExtras.MonoMeta,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            row.nama?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExtraRows(row.extras)
        }
        row.sks?.let {
            Text(
                text = "$it SKS",
                style = ForfhTypeExtras.MonoMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

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
                    style = ForfhTypeExtras.MonoMeta,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            row.nama?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
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
                    style = MaterialTheme.typography.bodySmall,
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

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
                    style = MaterialTheme.typography.titleSmall,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExtraRows(row.extras)
        }
        InfoFormat.formatRupiah(row.nominal)?.let {
            Text(
                text = it,
                style = ForfhTypeExtras.MonoMeta,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun CalendarRowView(row: CalendarRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        row.kegiatan?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ExtraRows(row.extras)
    }
}

@Composable
private fun SummaryCardView(model: SummaryCardModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (model.headline.isNotEmpty()) {
            FactGrid(model.headline)
        }
        if (model.headline.isNotEmpty() && model.rows.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
        model.rows.forEach { (label, value) -> FactRow(label, value) }
    }
}

@Composable
private fun DosenWaliView(d: DosenFacts) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        d.nama?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (d.facts.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            d.facts.forEach { (label, value) -> FactRow(label, value) }
        }
    }
}

@Composable
private fun InstructionBlockView(model: InstructionBlockModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        model.courses.forEachIndexed { i, course ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                course.kode?.let {
                    Text(
                        text = it,
                        style = ForfhTypeExtras.MonoMeta,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                course.nama?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                course.sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        section.nama?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        section.teks?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun GenericRowsView(rows: InfoRows) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.blocks.forEachIndexed { i, block ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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

@Composable
private fun FactRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
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

@Composable
private fun ExtraRows(extras: List<Pair<String, String>>) {
    if (extras.isEmpty()) return
    Text(
        text = extras.joinToString(" · ") { (label, value) -> "$label $value" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
