import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.onExternalDrag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import comms.IPV8Wrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    val ipv8 = IPV8Wrapper()
    val ipv8Job = CoroutineScope(Dispatchers.Default).launch {
        println("ipv8 starting...")
        ipv8.run()
        println("ipv8 finished")
    }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
        TextField(
            value = text,
            onValueChange = { text = it },
            Modifier.onExternalDrag(
                onDragStart = { externalDragValue -> println("Dragged! ${externalDragValue.dragData}") },
                onDragExit = { println("Dragged out!") },
                onDrop = { externalDropValue ->
                    val dragData = externalDropValue.dragData
                    if (dragData is DragData.FilesList) {
                        val filePaths = dragData.readFiles()
                        println("Dropped! ${filePaths.firstOrNull()}")
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
