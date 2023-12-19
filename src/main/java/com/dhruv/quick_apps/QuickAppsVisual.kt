package com.dhruv.quick_apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

@Composable
fun QuickAppsVisual(
    modifier: Modifier,
    viewModel: QuickAppsViewModel,
    alphabetSideFloat: Float,
    labelSize: Float,
    appComposable: @Composable (action: Action, offset: IntOffset, selected: Boolean) -> Unit,
    wordsBGComposable: @Composable() ((offset: IntOffset, size: IntSize, selectionHeight: Int) -> Unit)?,
    iconsBGComposable: @Composable() ((offset: IntOffset, size: IntSize, selectionHeight: Int) -> Unit)?,
    triggerClosedBGComposable: @Composable() ((offset: IntOffset, size: IntSize, selectionHeight: Int) -> Unit)?,
){
    val offsetsChange by remember (viewModel.selectedString){
        mutableStateOf(viewModel.getIconsOffsetsChange)
    }
    val actions by remember (viewModel.selectedString){
        mutableStateOf(viewModel.getActionsForAlphabet)
    }
    val baseOffset by remember {
        mutableStateOf(IntOffset(-viewModel.sidePadding.toInt(), -(viewModel.rowHeight/3).toInt()))
    }
    val selectedYOffset by animateFloatAsState(targetValue = viewModel.getSelectedStringYOffset, label = "selected-string-Y-offset")
    val currentAlphabet = viewModel.selectedString
    val alphabetOffsets = getAnimatedAlphabetOffset(viewModel.getAlphabetYOffsets,alphabetSideFloat,currentAlphabet, viewModel.getTriggerSize)

    @Composable
    fun animatedAlphabet(s: String){
        Box(
            modifier = Modifier
                .offset { alphabetOffsets[s]!!.value }
        ) {
            Text(text = s, style = TextStyle(color = Color.White, fontSize = TextUnit(labelSize, TextUnitType.Sp)))
        }
    }

    @Composable
    fun allAlphabets(){
        for ((i, _) in viewModel.getAlphabetYOffsets){
            animatedAlphabet(s = i)
        }
    }

    @Composable
    fun selectedAlphabetApps(){
        Box(
            modifier = Modifier
        ){
            for (i in actions.indices){
                val offsetChange = offsetsChange[i]
                val action = actions[i]
                val iconOffset = baseOffset + Offset(0f, selectedYOffset).round() + offsetChange.round()
                val isSelected = viewModel.currentAction!=null && (actions[i].name == viewModel.currentAction!!.name)
                if(isSelected){
                    viewModel.currentActionOfset = iconOffset.toOffset()
                }
                appComposable(
                    action,
                    iconOffset,
                    isSelected,
                )
            }
        }
    }

    Box(
        modifier = modifier
            .offset { viewModel.getTriggerOffset }
            .size(viewModel.getTriggerSize.width.dp, viewModel.getTriggerSize.height.dp)
    ){
        when (viewModel.selectionMode) {
            SelectionMode.NonActive -> {
//                Box (
//                    modifier = Modifier
//                        .offset { -viewModel.getTriggerOffset }
//                ){
//                    triggerClosedBGComposable?.invoke(
//                        viewModel.getTriggerOffset,
//                        viewModel.getTriggerSize,
//                        selectedYOffset.toInt()
//                    )
//                }
                allAlphabets()
            }
            SelectionMode.CharSelect -> {
                Box (
                    modifier = Modifier
                    .offset { -viewModel.getTriggerOffset }
                ){
                    wordsBGComposable?.invoke(
                        viewModel.getTriggerOffset,
                        viewModel.getTriggerSize,
                        selectedYOffset.toInt()
                    )
                }
                allAlphabets()
                selectedAlphabetApps()
            }
            SelectionMode.AppSelect -> {
                animatedAlphabet(s = viewModel.selectedString)
                Box (
                    modifier = Modifier
                        .offset { -viewModel.getTriggerOffset }
                ){
                    iconsBGComposable?.invoke(
                        viewModel.getTriggerOffset,
                        viewModel.getTriggerSize,
                        selectedYOffset.toInt()
                    )
                }
                allAlphabets()
                selectedAlphabetApps()
            }
        }
    }
}

@Composable
fun getAnimatedAlphabetOffset(AlphabetYOffsets: Map<String, Float>, alphabetSideFloat: Float, currentAlphabet: String, triggerSize: IntSize): Map<String, State<IntOffset>> {
    val alphabetOffsets = mutableMapOf<String, State<IntOffset>>()
    for ((alphabet, yOff) in AlphabetYOffsets) {
        alphabetOffsets[alphabet] = animateIntOffsetAsState(
            targetValue =
                if (alphabet == currentAlphabet) IntOffset(-alphabetSideFloat.toInt()-15, yOff.toInt() - 25)
                else IntOffset((triggerSize.width/2), yOff.toInt()),
            label = "animateAlphabetOffset{$alphabet}",
            animationSpec = tween(durationMillis = 50)
        )
    }
    return alphabetOffsets.toMap()
}