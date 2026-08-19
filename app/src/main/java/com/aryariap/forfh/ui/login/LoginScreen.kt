package com.aryariap.forfh.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryariap.forfh.R
import com.aryariap.forfh.ui.theme.ErrorBox
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.PrimaryButton

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.success) { if (state.success) Unit }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForfhColors.PitchBlack),
    ) {
        // Subtle ambient indigo glow at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ForfhColors.LinearIndigo.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(16.dp))

            // Official Logo Container
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ForfhColors.SurfaceElevated)
                    .border(1.dp, ForfhColors.BorderStrong, RoundedCornerShape(22.dp))
                    .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = ForfhColors.LinearIndigo.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "Logo ForFH",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "ForFH",
                style = MaterialTheme.typography.headlineLarge,
                color = ForfhColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ForfhColors.LinearIndigoSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, ForfhColors.LinearIndigo.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = "FAKULTAS HUKUM · UNAIR",
                    style = ForfhTypeExtras.MonoMeta,
                    color = ForfhColors.LinearIndigo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Sinkronisasi jadwal kuliah, tugas, dan presensi akademik Cybercampus.",
                style = MaterialTheme.typography.bodySmall,
                color = ForfhColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(28.dp))

            // Elevated Credentials Card
            ForfhSurface(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = ForfhColors.SurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForfhColors.BorderSubtle),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Masuk ke Akun",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForfhColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Cybercampus") },
                        placeholder = { Text("nama@fh.unair.ac.id") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = ForfhColors.TextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForfhColors.LinearIndigo,
                            unfocusedBorderColor = ForfhColors.BorderSubtle,
                            focusedContainerColor = ForfhColors.SurfaceSecondary,
                            unfocusedContainerColor = ForfhColors.SurfaceSecondary,
                            focusedTextColor = ForfhColors.TextPrimary,
                            unfocusedTextColor = ForfhColors.TextPrimary,
                            focusedLabelColor = ForfhColors.LinearIndigo,
                            unfocusedLabelColor = ForfhColors.TextMuted,
                            cursorColor = ForfhColors.LinearIndigo,
                        ),
                        enabled = !state.loading,
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ForfhColors.TextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = if (passwordVisible) "Sembunyi" else "Lihat",
                                style = ForfhTypeExtras.MonoMeta,
                                color = ForfhColors.LinearIndigo,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { passwordVisible = !passwordVisible }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (state.email.isNotBlank() && state.password.isNotBlank()) {
                                    viewModel.login()
                                }
                            },
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForfhColors.LinearIndigo,
                            unfocusedBorderColor = ForfhColors.BorderSubtle,
                            focusedContainerColor = ForfhColors.SurfaceSecondary,
                            unfocusedContainerColor = ForfhColors.SurfaceSecondary,
                            focusedTextColor = ForfhColors.TextPrimary,
                            unfocusedTextColor = ForfhColors.TextPrimary,
                            focusedLabelColor = ForfhColors.LinearIndigo,
                            unfocusedLabelColor = ForfhColors.TextMuted,
                            cursorColor = ForfhColors.LinearIndigo,
                        ),
                        enabled = !state.loading,
                    )

                    ErrorBox(message = state.error)

                    PrimaryButton(
                        text = if (state.loading) "Memverifikasi Kredensial..." else "Masuk ke ForFH",
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login()
                        },
                        enabled = !state.loading && state.email.isNotBlank() && state.password.isNotBlank(),
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                        icon = if (state.loading) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            }
                        } else null,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Kredensial Anda disimpan secara aman di Keystore perangkat dan hanya digunakan untuk sinkronisasi Cybercampus FH UNAIR.",
                style = MaterialTheme.typography.bodySmall,
                color = ForfhColors.TextQuaternary,
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
