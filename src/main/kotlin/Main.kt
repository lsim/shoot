import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.onExternalDrag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import comms.IPV8Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val scope = CoroutineScope(Dispatchers.Default)
private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    val ipv8 = IPV8Wrapper()
    val ipv8Job = scope.launch {
        logger.info { "ipv8 starting..." }
        ipv8.run()
        logger.info { "ipv8 finished" }
    }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            Modifier.onExternalDrag(
                onDragStart = { externalDragValue -> logger.info { "Dragged! ${externalDragValue.dragData}" } },
                onDragExit = { logger.info { "Dragged out!" } },
                onDrop = { externalDropValue ->
                    val dragData = externalDropValue.dragData
                    if (dragData is DragData.FilesList) {
                        val filePaths = dragData.readFiles()
                        logger.info { "Dropped! ${filePaths.firstOrNull()}" }
                        text = filePaths.firstOrNull() ?: ""
                    }
                },
            ),
        )
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
