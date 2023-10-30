package com.dhruv.quick_apps

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.ViewModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * @param onAlphabetSelectionChange it can be used to add haptic feedback
 * @param onAppSelectionChange it can be used to add haptic feedback
 */
class QuickAppsViewModel(
    val onAlphabetSelectionChange: (alphabet: String)->Unit,
    val onAppSelectionChange: (action: Action?)->Unit,
    val actionsMap: Map<String, List<Action>>,
    val rowHeight: Double,
    val distanceBetweenIcons: Double,
    val sidePadding: Float,
    val topMinValue: Float = -50F,
    val leftMinValue: Float = -880F
): ViewModel() {
    var touchPosition by mutableStateOf(Offset.Zero)
    var currentAlphabet by mutableStateOf("")

    private var refA by mutableStateOf(Offset.Zero)
    private var refB by mutableStateOf(Offset.Zero)
    var currentAngle by mutableStateOf(0f)
    var currentDistance by mutableStateOf(0f)

    var currentRow by mutableStateOf(0)
    var currentColumn by mutableStateOf(0)
    var currentAction by mutableStateOf<Action?>(null)


    var selectionMode by mutableStateOf(SelectionMode.NonActive)

    fun onTap(){

    }

    fun onDragStart(offset: Offset) {
        touchPosition = offset
        selectionMode = SelectionMode.CharSelect
    }

    fun onDrag(change: Offset) {
        touchPosition += change
        when (selectionMode) {
            SelectionMode.NonActive -> {}
            SelectionMode.CharSelect -> {
                if(touchPosition.x < toAppSelectX){
                    selectionMode = SelectionMode.AppSelect
                    refA = Offset( -(sidePadding + 1),alphabetYOffsets[currentAlphabet] as Float)
                    refB = Offset( -sidePadding,alphabetYOffsets[currentAlphabet] as Float)
                }
                currentAlphabet = getCurrentAlphabet(touchPosition.y)
            }
            SelectionMode.AppSelect -> {
                if(touchPosition.x > toCharSelectX){
                    selectionMode = SelectionMode.CharSelect
                }
                currentAngle = -(calculateAngle(refA, refB, touchPosition) - 270)
                currentDistance = calculateDistance(refB, touchPosition)

                currentRow = (currentDistance/rowHeight).toInt()
                val deltaAngle = calculateAngleOnCircle((currentRow+1) * rowHeight, distanceBetweenIcons)
                currentColumn = (currentAngle/deltaAngle).toInt()
                var nextAction: Action? = null
                if(alphabetPositionActionMap[currentAlphabet] == null) nextAction = null
                else if (alphabetPositionActionMap[currentAlphabet]!![currentRow] == null) nextAction = null
                else if (alphabetPositionActionMap[currentAlphabet]!![currentRow]!![currentColumn] == null) nextAction = null
                else nextAction = allActions[alphabetPositionActionMap[currentAlphabet]!![currentRow]!![currentColumn]!!]
                if (currentAction != nextAction){
                    currentAction = nextAction
                    onAppSelectionChange(currentAction)
                }
            }
        }
    }

    fun onDragStop(context: Context){
        if(currentAction != null){
            currentAction!!.onSelect(context)
            currentAction = null
        }
        selectionMode = SelectionMode.NonActive
        currentAlphabet = ""
        currentDistance = 0f
        currentAngle    = 0f
        currentRow = 0
        currentColumn = 0
    }

    fun onTriggerGloballyPositioned(layoutCoordinates: LayoutCoordinates) {
        triggerSize = layoutCoordinates.size
        triggerOffset = layoutCoordinates.positionInRoot()
        handleIconsPositioningCalculations(leftMinValue = leftMinValue, topMinValue = topMinValue)
    }

    private fun getCurrentAlphabet(position: Float): String {
        val delta = triggerSize.height / actionsMap.size
        val index = (position / delta).toInt()
        val currIndex = clamp(index, 0, alphabetIconOffsets.size-1)
        val curr = actionsMap.keys.toList()[currIndex]
        if (curr != currentAlphabet){
            onAlphabetSelectionChange(curr)
        }
        return curr
    }

    private fun getPositionOnCircle(radius: Double, angleDegrees: Double): Offset {
        if (radius <= 0.0) {
            throw IllegalArgumentException("Radius must be a positive value")
        }
        val angleRadians = Math.toRadians(angleDegrees)
        val x = radius * cos(angleRadians)
        val y = radius * sin(angleRadians)

        return Offset(x.toFloat(), y.toFloat())
    }

    private fun handleIconsPositioningCalculations(topMinValue: Float = -50F, leftMinValue: Float = -880F){

        if (coordinateGenerated) return

        fun generateAlphabetYOffsets(){
            val map = mutableMapOf<String,Float>()
            val delta = triggerSize.height/actionsMap.size
            var curr = 0F
            for ((s, _) in actionsMap) {
                map[s] = curr - delta/2
                curr += delta
            }
            alphabetYOffsets = map.toMap()
        }

        fun generateIconCoordinates(
            startingRadius: Double = 1.0,
            radiusDiff: Double = 40.0,
            iconDistance: Double = 30.0,
            rounds: Int = 10,
            startingAngle:Double = -90.0,
            endAngle: Double = 90.0
        ){
            var row = 0
            var radius = startingRadius
            val iconCoordinates = mutableListOf<IconCoordinate>()
            val indexToCoordinates = mutableListOf<List<Int>>()
            for (i in 0 until rounds){
                val angle = calculateAngleOnCircle(radius, iconDistance)
                var col = 0
                var curr = startingAngle
                while (curr < endAngle){
                    iconCoordinates.add(IconCoordinate(radius,-curr - angle/2))
                    indexToCoordinates.add(listOf(row,col))
                    curr += angle
                    col++
                }
                radius += radiusDiff
                row++
            }
            allIconCoordinates = iconCoordinates.toList()
            indexToRowAndColumn = indexToCoordinates.toList()
        }

        fun generateAllActions(){
            val actionsL = mutableListOf<Action>()
            for ((_, actions) in actionsMap) {
                for (action in actions) {
                    actionsL.add(action)
                }
            }
            allActions = actionsL.toList()
        }

        fun generateIconMap(
            bottomMaxValue: Float = 1400F,
            topMinValue: Float = -50F,
            leftMinValue: Float = -880F
        ){
            val iconsM = mutableMapOf<String,List<Int>>();
            val positionToActionIndexMap: MutableMap<String, MutableMap<Int, MutableMap<Int, Int>>> = mutableMapOf()
            val maxRadius = mutableMapOf<String, Float>()
            var c_i: Int
            var action_i = 0
            for ((s, actions) in actionsMap){
                c_i = 0
                val origin = alphabetYOffsets[s] as Float
                val iconsL = mutableListOf<Int>()
                for (action in actions) {
                    var found = false
                    while (!found){
                        val c = allIconCoordinates[c_i]
                        val offset = Offset(0F, origin) + getPositionOnCircle(c.distance, c.angle)
                        if( offset.x < leftMinValue || offset.y > bottomMaxValue || offset.y < topMinValue ) {
                            c_i++
                            continue
                        }
                        found = true
                    }
                    iconsL.add(c_i)
                    val r_c = indexToRowAndColumn[c_i]
                    if(positionToActionIndexMap[s] == null)
                        positionToActionIndexMap[s] = mutableMapOf()
                    if(positionToActionIndexMap[s]!![r_c[0]] == null)
                        positionToActionIndexMap[s]!![r_c[0]] = mutableMapOf()
                    positionToActionIndexMap[s]!![r_c[0]]!![r_c[1]] = action_i
                    c_i++
                    action_i++
                }
                iconsM[s] = iconsL
                maxRadius[s] = allIconCoordinates[iconsL.last()].distance.toFloat()
            }
            alphabetIconsMap = iconsM
            alphabetPositionActionMap = positionToActionIndexMap.toMap()
            alphabetMaxAppsRadius = maxRadius.toMap()
        }

        fun generateActionPositions(){
            val list = mutableMapOf<String,List<Offset>>()

            for ((s, icons) in alphabetIconsMap){
                val l = mutableListOf<Offset>()
                for (p in icons){
                    val cpos = getPositionOnCircle(allIconCoordinates[p].distance, -allIconCoordinates[p].angle)
                    val finalPos = -cpos
                    l.add( finalPos )
                }
                list[s] = l.toList()
            }
            alphabetIconOffsets = list.toMap()
        }

        generateAlphabetYOffsets()
        generateAllActions()

        generateIconCoordinates(startingRadius = rowHeight, radiusDiff = rowHeight, iconDistance = distanceBetweenIcons)
        generateIconMap(bottomMaxValue = triggerSize.height.toFloat() - rowHeight.toFloat(), topMinValue= topMinValue, leftMinValue = leftMinValue)
        generateActionPositions()

        coordinateGenerated = true
    }

    val getSelectedStringYOffset: Float
        get() = alphabetYOffsets[currentAlphabet] ?: 0f
    val getAlphabetYOffsets: Map<String,Float>
        get() = alphabetYOffsets
    val getIconsOffsetsChange: List<Offset>
        get() = alphabetIconOffsets[currentAlphabet] ?: listOf()
    val getActionsForAlphabet : List<Action>
        get() = actionsMap[currentAlphabet] ?: listOf()
    val getTriggerOffset : IntOffset
        get() = triggerOffset.round()
    val getTriggerSize : IntSize
        get() = triggerSize
    val getAlphabetAppsMaxRadius : Float
        get() = alphabetMaxAppsRadius[currentAlphabet] ?: 0f
    val getGlobalTouchPosition: Offset
        get() = triggerOffset + touchPosition

    companion object{
        val toAppSelectX = -10
        val toCharSelectX = 0
        var triggerSize = IntSize(0,0)
        var triggerOffset = Offset(0f,0f)

        var coordinateGenerated = false
        var alphabetYOffsets: Map<String,Float> = mapOf()
        var allIconCoordinates: List<IconCoordinate> = listOf()
        var indexToRowAndColumn: List<List<Int>> = listOf()
        var allActions: List<Action> = listOf()
        var alphabetIconsMap: Map<String,List<Int>> = mapOf()
        var alphabetIconOffsets: Map<String,List<Offset>> = mapOf()
        var alphabetMaxAppsRadius: Map<String, Float> = mapOf()
        var alphabetPositionActionMap: Map<String, Map<Int, Map<Int, Int>>> = mapOf()
    }

    data class IconCoordinate(val distance: Double, val angle: Double)
}