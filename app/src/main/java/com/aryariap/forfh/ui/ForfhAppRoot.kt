package com.aryariap.forfh.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.ForfhApp
import com.aryariap.forfh.data.prefs.SessionEvent
import com.aryariap.forfh.ui.jadwal.JadwalScreen
import com.aryariap.forfh.ui.jadwal.JadwalViewModel
import com.aryariap.forfh.ui.login.LoginScreen
import com.aryariap.forfh.ui.login.LoginViewModel
import com.aryariap.forfh.ui.pengaturan.PengaturanScreen
import com.aryariap.forfh.ui.pengaturan.PengaturanViewModel
import com.aryariap.forfh.ui.tugas.TugasListScreen
import com.aryariap.forfh.ui.tugas.TugasViewModel

private object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
}

/** Factory sederhana: ViewModel dibangun dari AppContainer (bukan AndroidViewModel). */
fun <T : ViewModel> simpleFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
    }

@Composable
fun ForfhAppRoot(container: AppContainer, openTasks: Boolean) {
    val navController = rememberNavController()

    // Jalur awal: cek cookie sesi (terenkripsi di Keystore) — splash sesaat lalu login/main
    LaunchedEffect(Unit) {
        val loggedIn = container.sessionManager.isLoggedIn()
        navController.navigate(if (loggedIn) Routes.MAIN else Routes.LOGIN) { popUpTo(0) }
    }

    // Auto-logout (401) & login sukses → pindah halaman (spec §10)
    LaunchedEffect(Unit) {
        container.sessionManager.events.collect { ev ->
            when (ev) {
                SessionEvent.LoggedIn ->
                    navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                is SessionEvent.LoggedOut ->
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    NavHost(navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { /* sesaat, lalu navigate oleh efek di atas */ }
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = simpleFactory { LoginViewModel(container) })
            LoginScreen(vm)
        }
        composable(Routes.MAIN) {
            MainScaffold(container = container, openTasks = openTasks)
        }
    }
}

@Composable
private fun MainScaffold(container: AppContainer, openTasks: Boolean) {
    var tab by rememberSaveable { mutableIntStateOf(if (openTasks) 1 else 0) }
    val context = LocalContext.current
    val containerApp = (context.applicationContext as ForfhApp).container

    val jadwalVm: JadwalViewModel = viewModel(factory = simpleFactory { JadwalViewModel(containerApp) })
    val tugasVm: TugasViewModel = viewModel(factory = simpleFactory { TugasViewModel(containerApp) })
    val pengaturanVm: PengaturanViewModel = viewModel(factory = simpleFactory { PengaturanViewModel(containerApp) })

    val tabs: List<Triple<String, ImageVector, @Composable () -> Unit>> = listOf(
        Triple("Jadwal", Icons.Filled.DateRange as ImageVector) { JadwalScreen(jadwalVm) },
        Triple("Tugas", Icons.AutoMirrored.Filled.List) { TugasListScreen(tugasVm) },
        Triple("Atur", Icons.Filled.Settings) { PengaturanScreen(pengaturanVm) },
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, icon, _) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> JadwalScreen(jadwalVm)
                1 -> TugasListScreen(tugasVm)
                2 -> PengaturanScreen(pengaturanVm)
            }
        }
    }
}
