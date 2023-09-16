
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import comms.IPV8Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val scope = CoroutineScope(Dispatchers.Default)
private val logger = KotlinLogging.logger {}

@Composable
@Preview
fun AppPreview() {
    val preferences = ShootPreferences(true)
    App(IPV8Wrapper(preferences), preferences)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(ipv8Wrapper: IPV8Wrapper, preferences: ShootPreferences) {
    var outputDir by remember { mutableStateOf(preferences["outputDir", ""]) }
    val droppedPaths = remember { mutableStateListOf<String>() }
    var showDirPicker by remember { mutableStateOf(false) }
    var indicatingGreen by remember { mutableStateOf(false) }
    val color = updateTransition(indicatingGreen, label = "color")
        .animateColor { state ->
            if (state) Color.Green else Color.White
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
                            logger.info { "Dropped! ${filePaths.firstOrNull()}" }
                            droppedPaths.clear()
                            droppedPaths.addAll(filePaths)
                        }
                    },

                )
                .background(color.value),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextField(
                    value = droppedPaths.joinToString("\n"),
                    onValueChange = { outputDir = it },
                    Modifier.weight(0.5f),
                )
                Spacer(Modifier.weight(0.1f))
                Button(
                    onClick = { showDirPicker = true },
                ) {
                    Text("Pick output folder")
                }
            }
            DirectoryPicker(showDirPicker) { path ->
                showDirPicker = false
                if (path != null) outputDir = path
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "Drop files anywhere!",
                    Modifier.padding(5.dp),
                )
            }
        }
    }
}

fun main() = application {
    val preferences = ShootPreferences()

    val ipv8 = IPV8Wrapper(preferences)
    val ipv8Job = scope.launch {
        logger.info { "ipv8 starting..." }
        ipv8.run()
        logger.info { "ipv8 finished" }
    }

    Window(onCloseRequest = ::exitApplication) {
        App(ipv8, preferences)
    }
}
