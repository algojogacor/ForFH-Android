package com.aryariap.forfh.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhTheme
import com.aryariap.forfh.ui.theme.ForfhTypeExtras

/** Layer alarm di atas lock screen — API 27+ setShowWhenLocked/setTurnScreenOn, API 26 fallback flags. */
class FullScreenAlarmActivity : ComponentActivity() {

    private val viewModel: FullScreenAlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        enableEdgeToEdge()

        val identity = intent.getStringExtra("identity") ?: run { finish(); return }
        val trigger = intent.getLongExtra("triggerAtMillis", -1L)
        if (trigger < 0) { finish(); return }
        viewModel.bind(identity, trigger)

        setContent {
            ForfhTheme(darkTheme = true) {
                AlarmUi(
                    viewModel = viewModel,
                    onSnooze = { viewModel.snooze() },
                    onClose = { viewModel.close(); finish() },
                )
            }
        }
    }
}

@Composable
private fun AlarmUi(
    viewModel: FullScreenAlarmViewModel,
    onSnooze: () -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LifecycleResumeEffect(Unit) {
        if (!state.valid) { /* finish dipicu dari state tidak valid */ }
        onPauseOrDispose { }
    }

    if (!state.valid) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForfhColors.AlarmBackground),
    ) {
        // Top 6dp accent strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(ForfhColors.AlarmBrass)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header & Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(ForfhColors.AlarmSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Alarm",
                        tint = ForfhColors.AlarmBrass,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Text(
                    text = "PENGINGAT KULIAH",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForfhColors.AlarmBrass,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                )
            }

            // Main Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = ForfhColors.AlarmSurface,
                border = BorderStroke(1.dp, ForfhColors.AlarmLine),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = state.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ForfhColors.DarkInk2,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.snoozeAvailable) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, ForfhColors.AlarmLine),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ForfhColors.DarkInk2,
                        ),
                    ) {
                        Text(
                            text = "Tidur lagi 3 menit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = ForfhColors.NavyDark,
                    ),
                ) {
                    Text(
                        text = "Tutup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}
