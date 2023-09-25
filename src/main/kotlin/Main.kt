
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import comms.IPV8Wrapper
import comms.ShootPeer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val scope = CoroutineScope(Dispatchers.Default)
private val uiScope = CoroutineScope(Dispatchers.Main)
private val logger = KotlinLogging.logger {}

@Composable
@Preview
fun AppPreview() {
    val preferences = ShootPreferences(true)
    App(IPV8Wrapper(preferences), preferences)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(ipv8: IPV8Wrapper, preferences: ShootPreferences) {
    var outputPath by remember { mutableStateOf(preferences["outputPath", preferences.defaultOutputPath]) }
//    val droppedPaths = remember { mutableStateListOf<String>() }
    var showDirPicker by remember { mutableStateOf(false) }
    var indicatingGreen by remember { mutableStateOf(false) }
    val color = updateTransition(indicatingGreen, label = "color")
        .animateColor { state ->
            if (state) Color.Green else Color.White
        }
//    val logLines = remember { mutableStateListOf<String>() }

    val currentPeers = remember { mutableStateListOf<ShootPeer>() }

//    val future = remember { mutableStateOf(CompletableFuture<ShootCommunity>()) }
//    val communityBinding = remember { mutableStateOf<ShootCommunity?>(null) }

//    future.value.thenAccept { community ->
//        logger.info { "Got community: ${community.serviceId}" }
//    }

    uiScope.launch {
        ipv8.shootPeerFlow
            .collect { peers ->
                currentPeers.clear()
                currentPeers.addAll(peers.values)
            }
    }

    MaterialTheme {
        Column(
            Modifier
                .fillMaxSize()
                .onExternalDrag(
                    onDragStart = { _ -> indicatingGreen = true },
                    onDragExit = { indicatingGreen = false },
                    onDrop = { externalDropValue ->
                        val dragData = externalDropValue.dragData
                        indicatingGreen = false
                        if (dragData is DragData.FilesList) {
                            val filePaths = dragData.readFiles()
                            logger.info { "Dropped! ${filePaths.firstOrNull()} currentPeers ${currentPeers.size}" }
                            for (path in filePaths) {
                                currentPeers.firstOrNull()?.sendFile(path)
                            }
                        }
                    },
                )
                .background(color.value),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Saving to $outputPath", Modifier.weight(0.1f))
                Button(
                    onClick = {
                        showDirPicker = true
                    },
                ) {
                    Text("Set output folder")
                }
            }
            DirectoryPicker(showDirPicker) { path ->
                showDirPicker = false
                if (path != null) {
                    outputPath = path
                    preferences["outputPath"] = path
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "Drop files anywhere!",
                    Modifier.padding(5.dp),
                )
            }
            Row(Modifier.fillMaxHeight()) {
                LazyColumn {
                    items(currentPeers.size) { index ->
                        Text("${currentPeers[index].name} (${currentPeers[index].peer.mid})")
                    }
                }
            }
        }
    }
}
var alreadyRunning = false
fun main() = application {
    if (alreadyRunning) {
        logger.info { "Already running!" }
    } else {
        logger.info { "First run" }
        alreadyRunning = true
    }
    val preferences = ShootPreferences()

    val ipv8 = IPV8Wrapper(preferences)
    scope.launch {
        logger.info { "ipv8 starting..." }
        ipv8.run()
        logger.info { "ipv8 finished" }
    }

    Window(onCloseRequest = ::exitApplication, title = "Shoot") {
        App(ipv8, preferences)
    }
}
