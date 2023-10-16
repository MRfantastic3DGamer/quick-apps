package com.dhruv.quick_apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

@Composable
fun QuickAppsVisual(
    modifier: Modifier,
    viewModel: QuickAppsViewModel,
    width: Double
){
    val offsets by remember (viewModel.currentAlphabet){
        mutableStateOf(viewModel.IconsOffsetsForAlphabet(viewModel.currentAlphabet)!!)
    }
    val actions by remember (viewModel.currentAlphabet){
        mutableStateOf(viewModel.ActionsForAlphabet(viewModel.currentAlphabet)!!)
    }
    val sidePadding by remember {
        mutableStateOf(IntOffset(-viewModel.sidePadding.toInt(), 0))
    }

    Box(
        modifier = modifier
    ){

        when (viewModel.selectionMode) {
            SelectionMode.NonActive -> {

            }
            SelectionMode.CharSelect -> {
                for ((i, yOff) in viewModel.AlphabetYOffsets){
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(5,yOff.toInt()) }
                    ) {
                        Text(text = i)
                    }
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(-100, viewModel.AlphabetYOffsets[viewModel.currentAlphabet]?.toInt() ?: 0) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp, 100.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Text(
                            text = viewModel.currentAlphabet,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .size(100.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
            SelectionMode.AppSelect -> {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(5, viewModel.AlphabetYOffsets[viewModel.currentAlphabet]?.toInt() ?: 0) }
                ) {
                    Text(text = viewModel.currentAlphabet)
                }
                for (i in actions.indices){
                    val offset = offsets[i]
                    val action = actions[i]
                    Box(
                        modifier = Modifier
                            .offset { sidePadding + offset.round() }
                            .size(20.dp)
                            .background(Color.White)
                    ){
                        Text(text = action.name)
                    }
                }
            }
        }
    }
}