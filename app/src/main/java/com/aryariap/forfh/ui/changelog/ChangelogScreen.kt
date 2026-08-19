package com.aryariap.forfh.ui.changelog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.data.changelog.ChangelogCatalog
import com.aryariap.forfh.data.changelog.ChangelogEntry
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries = remember { ChangelogCatalog.loadAll(context) }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = ForfhColors.PitchBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RIWAYAT PEMBARUAN",
                            style = ForfhTypeExtras.MonoMeta,
                            color = ForfhColors.LinearIndigo,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Catatan Perubahan",
                            style = MaterialTheme.typography.titleLarge,
                            color = ForfhColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = ForfhColors.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForfhColors.PitchBlack,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Daftar fitur baru, perbaikan, dan peningkatan performa pada aplikasi ForFH.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForfhColors.TextMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            items(entries, key = { it.version }) { entry ->
                ChangelogCard(entry = entry, isCurrentVersion = entry.version == BuildConfig.VERSION_NAME)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ChangelogCard(
    entry: ChangelogEntry,
    isCurrentVersion: Boolean,
    modifier: Modifier = Modifier,
) {
    ForfhSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Version Header & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Version Pill
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

                    if (isCurrentVersion) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ForfhColors.StatusSelesaiBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForfhColors.StatusSelesaiFg.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = "TERPASANG",
                                style = ForfhTypeExtras.MonoMeta,
                                color = ForfhColors.StatusSelesaiFg,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                Text(
                    text = entry.date,
                    style = ForfhTypeExtras.MonoMeta,
                    color = ForfhColors.TextMuted,
                )
            }

            // Title
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = ForfhColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )

            // Highlights
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }
    }
}
