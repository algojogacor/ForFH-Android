package com.aryariap.forfh.ui.changelog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryariap.forfh.data.changelog.ChangelogEntry
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.OutlineButton
import com.aryariap.forfh.ui.theme.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
    entry: ChangelogEntry,
    onDismiss: () -> Unit,
    onViewAllHistory: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ForfhColors.SurfaceElevated,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ForfhColors.BorderStrong),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Tag & Version
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ForfhColors.LinearIndigoSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForfhColors.LinearIndigo.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = "v${entry.version}",
                        style = ForfhTypeExtras.MonoMeta,
                        color = ForfhColors.LinearIndigo,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }

                Text(
                    text = "PEMBARUAN TERBARU",
                    style = ForfhTypeExtras.MonoMeta,
                    color = ForfhColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ada yang baru di ForFH!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ForfhColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForfhColors.TextMuted,
                )
            }

            // Highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ForfhColors.SurfaceSecondary)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (item in entry.highlights) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(ForfhColors.LinearIndigo),
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForfhColors.TextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = "Mengerti",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlineButton(
                    text = "Lihat Semua Riwayat Perubahan",
                    onClick = {
                        onDismiss()
                        onViewAllHistory()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
