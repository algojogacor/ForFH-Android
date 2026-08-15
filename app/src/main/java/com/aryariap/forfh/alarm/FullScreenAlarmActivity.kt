package com.aryariap.forfh.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aryariap.forfh.ui.theme.ForfhTheme

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
            ForfhTheme {
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

    // lifecycle-runtime-compose 2.11.0: member DSL onResume/onPause sudah dihapus —
    // blok effect dijalankan setiap lifecycle RESUMED (padanan onResume); onPauseOrDispose menggantikan onPause.
    LifecycleResumeEffect(Unit) {
        if (!state.valid) { /* finish dipicu dari state tidak valid */ }
        onPauseOrDispose { /* layar kosong → user keluar via back */ }
    }

    if (!state.valid) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Bangun!",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(text = state.title, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(text = state.body, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(48.dp))
        if (state.snoozeAvailable) {
            Button(onClick = onSnooze) {
                Text("Tidur lagi 3 menit")
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onClose) {
            Text("Tutup")
        }
    }
}
