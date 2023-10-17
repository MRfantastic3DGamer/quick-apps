package com.dhruv.quick_apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round

@Composable
fun QuickAppsVisual(
    modifier: Modifier,
    viewModel: QuickAppsViewModel,
    alphabetSideFloat: Float,
    appComposable: @Composable (action: Action, modifier: Modifier)->Unit,
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
    val appsFolderOpenValue by animateFloatAsState(
        targetValue = if (viewModel.selectionMode == SelectionMode.AppSelect) 1F else 0F,
        label = "appsFolderOpenValue"
    )
    val currentAlphabet = viewModel.currentAlphabet
    val alphabetOffsets = getAnimatedAlphabetOffset(viewModel.AlphabetYOffsets,alphabetSideFloat,currentAlphabet)

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
                            .offset { alphabetOffsets[i]!!.value }
                    ) {
                        Text(text = i)
                    }
                }
            }
            SelectionMode.AppSelect -> {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(-alphabetSideFloat.toInt(), viewModel.AlphabetYOffsets[viewModel.currentAlphabet]?.toInt() ?: 0) }
                ) {
                    Text(text = viewModel.currentAlphabet)
                }
                for (i in actions.indices){
                    val offset = offsets[i]
                    val action = actions[i]
                    appComposable(
                        modifier = Modifier
                            .offset { sidePadding + offset.round() }
                            .scale(appsFolderOpenValue),
                        action = action
                    )
                }
            }
        }
    }
}

@Composable
fun getAnimatedAlphabetOffset(AlphabetYOffsets: Map<String, Float>, alphabetSideFloat: Float, currentAlphabet: String): Map<String, State<IntOffset>> {
    val alphabetOffsets = mutableMapOf<String, State<IntOffset>>()
    for ((alphabet, yOff) in AlphabetYOffsets) {
        alphabetOffsets[alphabet] = animateIntOffsetAsState(targetValue =
        if (alphabet == currentAlphabet) IntOffset(-alphabetSideFloat.toInt(), yOff.toInt())
        else IntOffset(0, yOff.toInt()), label = "animateAlphabetOffset{$alphabet}"
        )
    }
    return alphabetOffsets.toMap()
}