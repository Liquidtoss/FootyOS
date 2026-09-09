package app.footyos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.view.MotionEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.sceneview.rememberView
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader

/** Offline, single-arm demonstration. Playback never advances the workout timer or counts reps. */
@Composable
fun FloorPressViewer(playing: Boolean = true) {
    var paused by remember { mutableStateOf(false) }
    var slow by remember { mutableStateOf(false) }
    var cameraPreset by remember { mutableIntStateOf(0) }
    var cameraRevision by remember { mutableIntStateOf(0) }
    var retry by remember { mutableIntStateOf(0) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running by rememberUpdatedState(playing && !paused && lifecycleState.isAtLeast(Lifecycle.State.RESUMED))
    val speed by rememberUpdatedState(if (slow) 0.5f else 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        key(retry) {
            val engine = rememberEngine()
            val loader = rememberModelLoader(engine)
            val result = remember(loader) {
                runCatching {
                    ModelNode(loader.createModelInstance("training3d/floor_press.glb"),
                        autoAnimate = false, scaleToUnits = 2f, centerOrigin = Position(0f))
                }
            }
            val model = result.getOrNull()
            if (model == null) {
                Text("3D preview couldn’t load. The form cues and reference video are still available.")
                TextButton(onClick = { retry++ }) { Text("Retry 3D preview") }
            } else {
                DisposableEffect(model) { onDispose { model.destroy() } }
                // Keep animation time outside the camera key: changing views never restarts a rep.
                val clock = remember(model) { PlaybackClock() }
                key(cameraRevision) {
                    val home = when (cameraPreset) {
                        1 -> Position(3.2f, 1.2f, 0f)
                        2 -> Position(0f, 1.2f, 3.2f)
                        else -> Position(2.5f, 2f, 2.5f)
                    }
                    val camera = rememberCameraNode(engine) {
                        position = home
                        lookAt(Position(0f))
                    }
                    val renderView = rememberView(engine).apply {
                        setShadowingEnabled(true)
                        ambientOcclusionOptions = ambientOcclusionOptions.apply { enabled = true }
                    }
                    var ready by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth().height(300.dp)) {
                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        view = renderView,
                        modelLoader = loader,
                        cameraNode = camera,
                        cameraManipulator = rememberCameraManipulator(home, Position(0f)),
                        childNodes = listOf(model),
                        onViewCreated = {
                            setOnTouchListener { view, event ->
                                view.parent?.requestDisallowInterceptTouchEvent(
                                    event.actionMasked != MotionEvent.ACTION_UP &&
                                        event.actionMasked != MotionEvent.ACTION_CANCEL)
                                false
                            }
                        },
                        onFrame = { nanos ->
                            if (!ready) ready = true
                            val delta = if (clock.last == 0L) 0f else
                                ((nanos - clock.last) / 1_000_000_000f).coerceIn(0f, .1f)
                            clock.last = nanos
                            if (running) clock.seconds += delta * speed
                            val duration = model.animator.getAnimationDuration(0)
                            if (duration > 0f) {
                                clock.seconds %= duration
                                model.animator.applyAnimation(0, clock.seconds)
                                model.animator.updateBoneMatrices()
                            }
                        },
                    )
                    if (!ready) Text("Loading 3D…", Modifier.align(Alignment.Center))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { paused = !paused }) { Text(if (paused) "Play demo" else "Pause demo") }
                    TextButton(onClick = { slow = !slow }) { Text(if (slow) "0.5×" else "1×") }
                    TextButton(onClick = { cameraPreset = 0; cameraRevision++ }) { Text("Reset view") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { cameraPreset = 1; cameraRevision++ }) { Text("Side") }
                    TextButton(onClick = { cameraPreset = 2; cameraRevision++ }) { Text("Front") }
                }
            }
        }
        Text("Drag to rotate • pinch to zoom", style = MaterialTheme.typography.labelSmall)
        Text("Left-arm demonstration • repeat on the other side", style = MaterialTheme.typography.bodySmall)
    }
}

private class PlaybackClock(var last: Long = 0L, var seconds: Float = 0f)
