package com.dhruv.quick_apps

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun QuickAppsTrigger(
    modifier: Modifier,
    viewModel: QuickAppsViewModel
){
    val haptic = LocalHapticFeedback.current
    viewModel.setHapticFeedback(haptic)
    val context = LocalContext.current
    Box(
        modifier = modifier
            .onGloballyPositioned {
                viewModel.onTriggerGloballyPositioned(it)
            }
            .pointerInput(Unit){
                detectDragGestures(
                    onDragStart = {
                        viewModel.onDragStart(it)
                    },
                    onDrag = { change, offset ->
                        change.consume()
                        viewModel.onDrag(offset)
                    },
                    onDragEnd = {
                        viewModel.onDragStop(context)
                    },
                    onDragCancel = {
                        viewModel.onDragStop(context)
                    }
                )
            },
    )
}
