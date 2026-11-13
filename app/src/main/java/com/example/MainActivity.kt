package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.GameState
import com.example.game.gl.GlTextureView
import com.example.ui.GameHudOverlay
import com.example.ui.GameViewModel
import com.example.ui.PauseMenuOverlay
import com.example.ui.SettingsModal
import com.example.ui.ShowroomMenuOverlay
import com.example.ui.SplashScreen
import com.example.ui.theme.HypercarResortTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
            var hasToggledFullscreen by remember { mutableStateOf(false) }

            LaunchedEffect(isFullscreen) {
                if (!isFullscreen && !hasToggledFullscreen) return@LaunchedEffect
                hasToggledFullscreen = true
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isFullscreen) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            HypercarResortTheme {
                HypercarGameScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.audioEngine.start()
    }

    override fun onPause() {
        viewModel.audioEngine.stop()
        super.onPause()
    }
}

@Composable
fun HypercarGameScreen(viewModel: GameViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler(
        enabled = isSettingsOpen ||
            telemetry.gameState == GameState.PAUSED ||
            telemetry.gameState == GameState.PLAYING
    ) {
        when {
            isSettingsOpen -> viewModel.closeSettings()
            telemetry.gameState == GameState.PAUSED -> viewModel.resumeGame()
            telemetry.gameState == GameState.PLAYING -> viewModel.pauseGame()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                val isDown = when (keyEvent.type) {
                    KeyEventType.KeyDown -> true
                    KeyEventType.KeyUp -> false
                    else -> return@onPreviewKeyEvent false
                }
                when (keyEvent.key) {
                    Key.Escape -> {
                        if (isDown) {
                            when {
                                isSettingsOpen -> viewModel.closeSettings()
                                telemetry.gameState == GameState.PLAYING -> viewModel.pauseGame()
                                telemetry.gameState == GameState.PAUSED -> viewModel.resumeGame()
                            }
                        }
                        true
                    }
                    Key.W, Key.DirectionUp -> {
                        viewModel.setKeyGas(isDown); true
                    }
                    Key.S, Key.DirectionDown -> {
                        viewModel.setKeyBrake(isDown); true
                    }
                    Key.A, Key.DirectionLeft -> {
                        viewModel.setKeySteerLeft(isDown); true
                    }
                    Key.D, Key.DirectionRight -> {
                        viewModel.setKeySteerRight(isDown); true
                    }
                    Key.Spacebar -> {
                        viewModel.setKeyHandbrake(isDown); true
                    }
                    Key.N, Key.ShiftLeft, Key.ShiftRight -> {
                        viewModel.setKeyNitro(isDown); true
                    }
                    Key.C -> {
                        if (isDown) viewModel.cycleCameraMode()
                        true
                    }
                    Key.T -> {
                        if (isDown) viewModel.cycleTimeOfDay()
                        true
                    }
                    Key.F -> {
                        if (isDown) viewModel.toggleFullscreen()
                        true
                    }
                    Key.R -> {
                        if (isDown) viewModel.resetCar()
                        true
                    }
                    Key.M -> {
                        if (isDown) viewModel.toggleMute()
                        true
                    }
                    else -> false
                }
            }
    ) {
        val glViewState = remember { mutableStateOf<GlTextureView?>(null) }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GlTextureView(ctx, viewModel.renderer).also {
                    glViewState.value = it
                }
            }
        )

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> glViewState.value?.onResume()
                    Lifecycle.Event.ON_PAUSE -> glViewState.value?.onPause()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        when (telemetry.gameState) {
            GameState.SPLASH -> {
                SplashScreen(onFinished = viewModel::finishSplash)
            }

            GameState.MENU -> {
                ShowroomMenuOverlay(
                    telemetry = telemetry,
                    settings = settings,
                    records = records,
                    onLaunchGame = viewModel::launchGame,
                    onSelectCar = viewModel::selectCar,
                    onSelectPaint = viewModel::selectPaintColor,
                    onCycleFinish = viewModel::cyclePaintFinish,
                    onCycleTimeOfDay = viewModel::cycleTimeOfDay,
                    onOpenSettings = viewModel::openSettings
                )
            }

            GameState.PLAYING, GameState.PAUSED -> {
                GameHudOverlay(
                    telemetryFlow = viewModel.telemetry,
                    settingsFlow = viewModel.settings,
                    isFullscreenFlow = viewModel.isFullscreen,
                    onPauseGame = viewModel::pauseGame,
                    onSelectCar = viewModel::selectCar,
                    onSelectPaint = viewModel::selectPaintColor,
                    onCycleFinish = viewModel::cyclePaintFinish,
                    onCycleCamera = viewModel::cycleCameraMode,
                    onCycleTimeOfDay = viewModel::cycleTimeOfDay,
                    onToggleFullscreen = viewModel::toggleFullscreen,
                    onToggleMute = viewModel::toggleMute,
                    onResetCar = viewModel::resetCar,
                    onTouchGas = viewModel::setTouchGas,
                    onTouchBrake = viewModel::setTouchBrake,
                    onTouchSteerLeft = viewModel::setTouchSteerLeft,
                    onTouchSteerRight = viewModel::setTouchSteerRight,
                    onTouchAnalogSteer = viewModel::setTouchAnalogSteer,
                    onTouchHandbrake = viewModel::setTouchHandbrake,
                    onTouchNitro = viewModel::setTouchNitro
                )

                AnimatedVisibility(
                    visible = telemetry.gameState == GameState.PAUSED && !isSettingsOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PauseMenuOverlay(
                        onResume = viewModel::resumeGame,
                        onResetCar = viewModel::resetCar,
                        onTeleport = viewModel::teleportTo,
                        onOpenSettings = viewModel::openSettings,
                        onExitToMenu = viewModel::exitToMenu
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SettingsModal(
                settings = settings,
                onUpdateSettings = viewModel::updateSettings,
                onClose = viewModel::closeSettings
            )
        }
    }
}
