package com.aryariap.forfh.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.aryariap.forfh.ui.info.InfoScreen
import com.aryariap.forfh.ui.info.InfoViewModel
import com.aryariap.forfh.ui.jadwal.JadwalScreen
import com.aryariap.forfh.ui.jadwal.JadwalViewModel
import com.aryariap.forfh.ui.jadwal.NextUpViewModel
import com.aryariap.forfh.ui.login.LoginScreen
import com.aryariap.forfh.ui.login.LoginViewModel
import com.aryariap.forfh.ui.pengaturan.PengaturanScreen
import com.aryariap.forfh.ui.pengaturan.PengaturanViewModel
import com.aryariap.forfh.ui.theme.ForfhNavDock
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
fun ForfhAppRoot(container: AppContainer, startTab: Int?) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        val loggedIn = container.sessionManager.isLoggedIn()
        navController.navigate(if (loggedIn) Routes.MAIN else Routes.LOGIN) { popUpTo(0) }
    }

    LaunchedEffect(Unit) {
        container.sessionManager.events.collect { ev ->
            when (ev) {
                SessionEvent.LoggedIn ->
                    navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                is SessionEvent.LoggedOut -> {
                    if (!ev.cleanupDone) container.logout(ev.message)
                    if (navController.currentDestination?.route != Routes.LOGIN) {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                }
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
            MainScaffold(container = container, startTab = startTab)
        }
    }
}

@Composable
private fun MainScaffold(container: AppContainer, startTab: Int?) {
    var tab by rememberSaveable { mutableIntStateOf(startTab ?: 0) }

    LaunchedEffect(startTab) {
        startTab?.let { if (it in 0..3) tab = it }
    }

    val context = LocalContext.current
    val containerApp = (context.applicationContext as ForfhApp).container

    val jadwalVm: JadwalViewModel = viewModel(factory = simpleFactory { JadwalViewModel(containerApp) })
    val nextUpVm: NextUpViewModel = viewModel(factory = simpleFactory { NextUpViewModel(containerApp) })
    val tugasVm: TugasViewModel = viewModel(factory = simpleFactory { TugasViewModel(containerApp) })
    val infoVm: InfoViewModel = viewModel(factory = simpleFactory { InfoViewModel(containerApp) })
    val pengaturanVm: PengaturanViewModel = viewModel(factory = simpleFactory { PengaturanViewModel(containerApp) })

    Scaffold(
        bottomBar = {
            ForfhNavDock(
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> JadwalScreen(jadwalVm, nextUpVm)
                1 -> TugasListScreen(tugasVm)
                2 -> InfoScreen(infoVm)
                3 -> PengaturanScreen(pengaturanVm)
            }
        }
    }
}
