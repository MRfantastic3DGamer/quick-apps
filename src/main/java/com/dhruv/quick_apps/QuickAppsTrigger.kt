package com.dhruv.quick_apps

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext

@Composable
fun QuickAppsTrigger(
    modifier: Modifier,
    onTriggerGloballyPositioned: (LayoutCoordinates) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragStop: () -> Unit
){
    val context = LocalContext.current
    Box(
        modifier = modifier
            .onGloballyPositioned {
                onTriggerGloballyPositioned(it)
            }
            .pointerInput(Unit){
                detectDragGestures(
                    onDragStart = {
                        onDragStart(it)
                    },
                    onDrag = { change, offset ->
                        change.consume()
                        onDrag(offset)
                    },
                    onDragEnd = {
                        onDragStop()
                    },
                    onDragCancel = {
                        onDragStop()
                    }
                )
            },
    )
}
