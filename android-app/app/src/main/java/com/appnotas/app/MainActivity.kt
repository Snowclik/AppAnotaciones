package com.appnotas.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appnotas.app.ui.ExplorerViewModel
import com.appnotas.app.ui.screens.ExplorerScreen
import com.appnotas.app.ui.theme.AppNotasTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ExplorerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            BackHandler(
                enabled = uiState.isFabMenuExpanded ||
                    uiState.isSearchExpanded ||
                    (uiState.folderView?.breadcrumbs?.size ?: 0) > 1
            ) {
                viewModel.navigateUp()
            }

            AppNotasTheme {
                ExplorerScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}
