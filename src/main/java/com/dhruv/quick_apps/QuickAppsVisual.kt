package com.dhruv.quick_apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

@Composable
fun QuickAppsVisual(
    modifier: Modifier,
    viewModel: QuickAppsViewModel,
    alphabetSideFloat: Float,
    labelSize: Float,
    appComposable:                  @Composable (action: Action, offset: IntOffset, selected: Boolean) -> Unit,
    triggerBGComposable:            @Composable() ((offset: IntOffset, size: IntSize, selectionHeight: Int) -> Unit)?,
    iconsBGComposable:              @Composable() ((offset: IntOffset, size: IntSize, selectionHeight: Int) -> Unit)?,
    groupLabelComposable:           @Composable() ((offset: IntOffset, size: Float, value: String) -> Unit),
    allActions: List<Action>
){
    val offsetsChange = viewModel.getIconsOffsetsChange
    val actions by remember (viewModel.selectedString){
        mutableStateOf(viewModel.currentActions)
    }
    val baseOffset by remember {
        mutableStateOf(IntOffset(-viewModel.sidePadding.toInt(), -(viewModel.rowHeight/3).toInt()))
    }
    val selectedYOffset by animateFloatAsState(targetValue = viewModel.getSelectedTriggerYOffset, label = "selected-string-Y-offset")
    val currentAlphabet = viewModel.selectedString
    val alphabetOffsets = getAnimatedAlphabetOffset(viewModel.currentTriggerYOffsets,alphabetSideFloat,currentAlphabet, viewModel.getTriggerSize)
    val selectionMode = viewModel.selectionMode

    @Composable
    fun animatedAlphabet (s: String) {
        if (alphabetOffsets[s] == null) return
        groupLabelComposable(alphabetOffsets[s]!!.value, 1f, s)
    }

    @Composable
    fun allAlphabets () {
        for ((i, _) in viewModel.currentTriggerYOffsets){
            animatedAlphabet(s = i)
        }
    }

    /**
     * icons for all the selected group/character
     */
    @Composable
    fun appIcons () {
        Box(
            modifier = Modifier
        ){
            for (i in actions.indices){
                val offsetChange = offsetsChange[i]
                val action = actions[i]
                val iconOffset = baseOffset + Offset(0f, selectedYOffset).round() + offsetChange.round()
                val isSelected = viewModel.currentAction!=null && (actions[i] == viewModel.currentAction!!)
                if(isSelected){
                    viewModel.currentActionOffset = iconOffset.toOffset()
                }
                appComposable(
                    allActions[action],
                    iconOffset,
                    isSelected,
                )
            }
        }
    }







    triggerBGComposable?.invoke(
        viewModel.getTriggerOffset,
        viewModel.getTriggerSize,
        selectedYOffset.toInt()
    )

    Box(
        modifier = modifier
            .offset { viewModel.getTriggerOffset }
            .size(viewModel.getTriggerSize.width.dp, viewModel.getTriggerSize.height.dp)
    ){

        // trigger mode selection
        AnimatedVisibility(
            visible = selectionMode == SelectionMode.NonActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val del = viewModel.getTriggerSize.height/4
            Icon(
                modifier = Modifier.offset { IntOffset(20, del) },
                imageVector = Icons.Default.Search,
                contentDescription = "search icon"
            )
            Icon(
                modifier = Modifier.offset { IntOffset(20, del*3) },
                imageVector = Icons.Default.Star,
                contentDescription = "search icon"
            )
        }

        // trigger labels
        AnimatedVisibility(
            visible = selectionMode != SelectionMode.NonActive,
            enter = slideIn { IntOffset(viewModel.getTriggerSize.width, 50) },
            exit = slideOut { IntOffset(viewModel.getTriggerSize.width, 50) }
        ) {
            allAlphabets()
            appIcons()
        }

        // app icons
        AnimatedVisibility(
            visible = selectionMode != SelectionMode.NonActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
            appIcons()
        }
    }
}

@Composable
fun getAnimatedAlphabetOffset(
    alphabetYOffsets: Map<String, Float>,
    alphabetSideFloat: Float,
    currentAlphabet: String,
    triggerSize: IntSize
): Map<String, State<IntOffset>> {
    val alphabetOffsets = mutableMapOf<String, State<IntOffset>>()
    for ((alphabet, yOff) in alphabetYOffsets) {
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