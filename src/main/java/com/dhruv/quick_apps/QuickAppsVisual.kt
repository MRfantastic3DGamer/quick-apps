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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round

@Composable
fun QuickAppsVisual(
    modifier: Modifier,
    viewModel: QuickAppsViewModel,
    alphabetSideFloat: Float,
    appComposable: @Composable (action: Action, modifier: Modifier, selected: Boolean)->Unit,
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
        targetValue = if (viewModel.selectionMode != SelectionMode.NonActive) 1F else 0F,
        label = "appsFolderOpenValue"
    )
    val currentAlphabet = viewModel.currentAlphabet
    val alphabetOffsets = getAnimatedAlphabetOffset(viewModel.AlphabetYOffsets,alphabetSideFloat,currentAlphabet)

    @Composable
    fun animatedAlphabet(s: String){
        Box(
            modifier = Modifier
                .offset { alphabetOffsets[s]!!.value }
        ) {
            Text(text = s)
        }
    }

    @Composable
    fun allAlphabets(){
        for ((i, yOff) in viewModel.AlphabetYOffsets){
            animatedAlphabet(s = i)
        }
    }

    @Composable
    fun selectedAlphabetApps(){
        for (i in actions.indices){
            val offset = offsets[i]
            val action = actions[i]
            appComposable(
                action,
                Modifier
                    .offset { sidePadding + offset.round() }
                ,
                (viewModel.currentAction!=null && (actions[i].name == viewModel.currentAction!!.name))
            )
        }
    }

    Box(
        modifier = modifier
    ){

        when (viewModel.selectionMode) {
            SelectionMode.NonActive -> {

            }
            SelectionMode.CharSelect -> {
                allAlphabets()
                selectedAlphabetApps()
            }
            SelectionMode.AppSelect -> {
                animatedAlphabet(s = viewModel.currentAlphabet)
                selectedAlphabetApps()
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