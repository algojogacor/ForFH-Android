package com.aryariap.forfh.ui.tugas

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.aryariap.forfh.network.SubtaskDto
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhStatusPill
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.OutlineButton
import com.aryariap.forfh.ui.theme.PrimaryButton
import java.time.ZoneId
import java.util.regex.Pattern
import kotlinx.serialization.json.Json

private val URL_PATTERN = Pattern.compile(
    "(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)",
    Pattern.CASE_INSENSITIVE
)

private fun extractUrls(text: String): List<String> {
    val matcher = URL_PATTERN.matcher(text)
    val urls = mutableListOf<String>()
    while (matcher.find()) {
        matcher.group(1)?.let { urls.add(it) }
    }
    return urls
}

private fun cleanHtmlText(html: String): String {
    return runCatching {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
    }.getOrDefault(html.replace(Regex("<[^>]*>"), " ").trim())
}

private fun openBrowser(context: Context, url: String) {
    try {
        val target = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target.trim())).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuka tautan: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private const val HEBAT_MY_COURSES_URL = "https://hebat.elearning.unair.ac.id/my/courses.php"

private fun resolveTaskUrl(item: TugasItem): Pair<String, String> {
    // 1. Cek apakah ada URL eksplisit di dalam deskripsi tugas (misal lampiran / link custom)
    val urlsInDesc = extractUrls(item.description.orEmpty())
    if (urlsInDesc.isNotEmpty()) {
        val u = urlsInDesc.first()
        val label = if (u.contains("hebat.elearning.unair.ac.id")) "Buka Modul Tugas di HEBAT" else "Buka Tautan Lampiran"
        return u to label
    }

    // 2. Default langsung ke halaman Kursus Mahasiswa di HEBAT
    return HEBAT_MY_COURSES_URL to "Buka di HEBAT e-Learning"
}

@Composable
fun TugasDetailScreen(viewModel: TugasViewModel, taskId: String) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")
    val item = state.detail
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = ForfhColors.PitchBlack,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForfhColors.PitchBlack)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = { viewModel.closeDetail() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = ForfhColors.TextPrimary,
                    )
                }
                Text(
                    text = "Detail Tugas",
                    style = MaterialTheme.typography.titleMedium,
                    color = ForfhColors.TextPrimary,
                )
            }
        },
        bottomBar = {
            if (item != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ForfhColors.PitchBlack)
                        .border(BorderStroke(1.dp, ForfhColors.BorderSubtle))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    if (item.status == "DONE") {
                        OutlineButton(
                            text = "Tandai Belum Selesai",
                            onClick = { viewModel.unmarkDone(item.id) },
                            height = 50.dp,
                        )
                    } else {
                        PrimaryButton(
                            text = "Tandai Selesai",
                            onClick = { viewModel.markDone(item.id) },
                            height = 50.dp,
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (item == null) {
                Text(
                    text = "Tugas tidak ditemukan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForfhColors.TextMuted,
                )
                return@Column
            }

            // Status & Priority Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val statusLabel = UiFormat.statusLabel(if (item.status == "DONE") "DONE" else item.computedStatus ?: item.status)
                val (statusBg, statusFg) = when (statusLabel) {
                    "Selesai" -> ForfhColors.StatusSelesaiBg to ForfhColors.StatusSelesaiFg
                    "Terlambat" -> ForfhColors.StatusTerlambatBg to ForfhColors.StatusTerlambatFg
                    "Proses" -> ForfhColors.StatusProsesBg to ForfhColors.StatusProsesFg
                    "Revisi" -> ForfhColors.StatusRevisiBg to ForfhColors.StatusRevisiFg
                    else -> ForfhColors.StatusBelumBg to ForfhColors.StatusBelumFg
                }
                ForfhStatusPill(text = statusLabel, foreground = statusFg, background = statusBg)

                val priorityColor = when (item.priority.lowercase()) {
                    "urgent", "p1" -> ForfhColors.PriorityP1
                    "high", "p2" -> ForfhColors.PriorityP2
                    "medium", "p3" -> ForfhColors.PriorityP3
                    else -> ForfhColors.PriorityP4
                }
                ForfhStatusPill(
                    text = "Prioritas ${item.priority.replaceFirstChar { it.uppercase() }}",
                    foreground = priorityColor,
                    background = priorityColor.copy(alpha = 0.15f),
                )
            }

            // Title & Course
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.courseName?.let {
                    Text(
                        text = "$it ${item.courseCode ?: ""}".trim(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForfhColors.LinearIndigo,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = ForfhColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Metadata Card (Deadline & Jam)
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "DEADLINE PENGUMPULAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForfhColors.TextMuted,
                        )
                        Text(
                            text = item.dueAt?.let { UiFormat.deadline(it, zone) } ?: "Tanpa deadline",
                            style = ForfhTypeExtras.MonoMeta,
                            color = if (item.computedStatus == "OVERDUE" && item.status != "DONE") ForfhColors.PriorityP1
                            else ForfhColors.LinearIndigo,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Resolve direct HEBAT assignment / event URL
            val (taskUrl, actionTitle) = resolveTaskUrl(item)
            val rawDesc = item.description.orEmpty()

            // Dedicated Action Card: Buka di HEBAT e-Learning
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openBrowser(context, taskUrl) },
                shape = RoundedCornerShape(10.dp),
                color = ForfhColors.LinearIndigo.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, ForfhColors.LinearIndigo.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForfhColors.LinearIndigo.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Buka HEBAT",
                            tint = ForfhColors.LinearIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = actionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = ForfhColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = taskUrl,
                            style = ForfhTypeExtras.MonoMeta,
                            color = ForfhColors.LinearIndigo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ForfhColors.LinearIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Description Card with Interactive Clickable URLs & Clean HTML
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "DESKRIPSI TUGAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForfhColors.TextMuted,
                    )

                    if (rawDesc.isBlank()) {
                        Text(
                            text = "Tidak ada deskripsi tambahan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ForfhColors.TextMuted,
                        )
                    } else {
                        FormattedDescriptionText(
                            rawText = rawDesc,
                            onUrlClick = { url -> openBrowser(context, url) }
                        )
                    }
                }
            }

            // Subtasks Section
            val subtasks = item.subtasksJson?.let {
                runCatching { Json.decodeFromString<List<SubtaskDto>>(it) }.getOrNull()
            } ?: emptyList()

            if (subtasks.isNotEmpty()) {
                ForfhSurface {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "SUBTUGAS (${subtasks.count { it.completed == 1 }}/${subtasks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForfhColors.TextMuted,
                        )
                        subtasks.forEach { st ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (st.completed == 1) ForfhColors.StatusSelesaiFg
                                             else ForfhColors.SurfaceSecondary
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (st.completed == 1) ForfhColors.StatusSelesaiFg
                                                else ForfhColors.BorderSubtle
                                            ),
                                            RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (st.completed == 1) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selesai",
                                            tint = ForfhColors.PitchBlack,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = st.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (st.completed == 1) ForfhColors.TextMuted else ForfhColors.TextPrimary,
                                    textDecoration = if (st.completed == 1) TextDecoration.LineThrough else null,
                                )
                            }
                        }
                    }
                }
            }

            state.message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.startsWith("Gagal")) ForfhColors.PriorityP1
                    else ForfhColors.LinearIndigo,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormattedDescriptionText(
    rawText: String,
    onUrlClick: (String) -> Unit
) {
    val cleanText = cleanHtmlText(rawText)
    val annotatedString = buildAnnotatedString {
        append(cleanText)
        val matcher = URL_PATTERN.matcher(cleanText)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val url = matcher.group()
            addStyle(
                style = SpanStyle(
                    color = ForfhColors.LinearIndigo,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                ),
                start = start,
                end = end
            )
            addStringAnnotation(
                tag = "URL",
                annotation = url,
                start = start,
                end = end
            )
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = ForfhColors.TextSecondary
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onUrlClick(annotation.item)
                }
        }
    )
}
