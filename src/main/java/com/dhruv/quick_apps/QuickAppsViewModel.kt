package com.dhruv.quick_apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import java.lang.Integer.max
import java.lang.Math.min
import kotlin.math.cos
import kotlin.math.sin

/**
 * @param onAlphabetSelectionChange it can be used to add haptic feedback
 */
class QuickAppsViewModel(
    val onAlphabetSelectionChange: (alphabet: String)->Unit,
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
            SelectionMode.NonActive -> TODO()
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
                currentAngle = calculateAngle(refA, refB, touchPosition) - 90
                currentDistance = calculateDistance(refB, touchPosition)

                currentRow = (currentDistance/rowHeight).toInt()
                val deltaAngle = calculateAngleOnCircle((currentRow+1) * rowHeight, distanceBetweenIcons)
                currentColumn = (currentAngle/deltaAngle).toInt()
                if(alphabetPositionActionMap[currentAlphabet] == null) currentAction = null
                else if (alphabetPositionActionMap[currentAlphabet]!![currentRow] == null) currentAction = null
                else if (alphabetPositionActionMap[currentAlphabet]!![currentRow]!![currentColumn] == null) currentAction = null
                else currentAction = allActions[alphabetPositionActionMap[currentAlphabet]!![currentRow]!![currentColumn]!!]
            }
        }
    }

    fun onDragStop(){
        if(currentAction != null){
            currentAction!!.onSelect()
        }
        selectionMode = SelectionMode.NonActive
        currentAlphabet = ""
        currentDistance = 0f
        currentAngle    = 0f
        currentRow = 0
        currentColumn = 0
    }

    fun onTriggerGloballyPositioned(layoutCoordinates: LayoutCoordinates) {
        triggerHeight = layoutCoordinates.size.height
        handleIconsPositioningCalculations(leftMinValue = leftMinValue, topMinValue = topMinValue)
    }

    private fun getCurrentAlphabet(position: Float): String {
        val delta = triggerHeight / actionsMap.size
        val index = (position / delta).toInt()
        val currIndex = min(26, max(0,(index)))
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
            val delta = triggerHeight/actionsMap.size
            var curr = 0F
            for ((s, _) in actionsMap) {
                map[s] = curr
                curr += delta
            }
            alphabetYOffsets = map.toMap()
        }

        fun generateIconCoordinates(startingRadius: Double = 1.0, radiusDiff: Double = 40.0, iconDistance: Double = 30.0, rounds: Int = 10, startingAngle:Double = -90.0, endAngle: Double = 90.0){
            var row = 0
            var radius = startingRadius
            val iconCoordinates = mutableListOf<IconCoordinate>()
            val indexToCoordinates = mutableListOf<List<Int>>()
            for (i in 0 until rounds){
                val angle = calculateAngleOnCircle(radius, iconDistance)
                var col = 0
                var curr = startingAngle
                while (curr < endAngle){
                    iconCoordinates.add(IconCoordinate(radius,-curr))
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

        fun generateIconMap(bottomMaxValue: Float = 1400F, topMinValue: Float = -50F, leftMinValue: Float = -880F){
            val iconsM = mutableMapOf<String,List<Int>>();
            val positionToActionIndexMap: MutableMap<String, MutableMap<Int, MutableMap<Int, Int>>> = mutableMapOf()
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
            }
            alphabetIconsMap = iconsM
            alphabetPositionActionMap = positionToActionIndexMap.toMap()
        }

        fun generateActionPositions(){
            val list = mutableMapOf<String,List<Offset>>()
            for ((s, icons) in alphabetIconsMap){
                val yOffset = alphabetYOffsets[s] as Float
                val l = mutableListOf<Offset>()
                for (p in icons){
                    val cpos = getPositionOnCircle(allIconCoordinates[p].distance, -allIconCoordinates[p].angle)
                    val new = Offset(-cpos.x, cpos.y)
                    l.add( Offset(new.x, yOffset + new.y) )
                }
                list[s] = l.toList()
            }
            alphabetIconOffsets = list.toMap()
        }

        generateAlphabetYOffsets()
        generateAllActions()

        generateIconCoordinates(startingRadius = rowHeight, radiusDiff = rowHeight, iconDistance = distanceBetweenIcons)
        generateIconMap(bottomMaxValue = triggerHeight.toFloat(), topMinValue= topMinValue, leftMinValue = leftMinValue)
        generateActionPositions()

        coordinateGenerated = true
    }

    val AlphabetYOffsets: Map<String,Float>
        get() = alphabetYOffsets
    fun IconsOffsetsForAlphabet(s: String) = if(s == "") listOf() else alphabetIconOffsets[s]
    fun ActionsForAlphabet(s: String) = if(s == "") listOf() else actionsMap[s]

    companion object{
        val toAppSelectX = -10
        val toCharSelectX = 0
        var triggerHeight = 1555

        var coordinateGenerated = false
        var alphabetYOffsets: Map<String,Float> = mapOf()
        var allIconCoordinates: List<IconCoordinate> = listOf()
        var indexToRowAndColumn: List<List<Int>> = listOf()
        var allActions: List<Action> = listOf()
        var alphabetIconsMap: Map<String,List<Int>> = mapOf()
        var alphabetIconOffsets: Map<String,List<Offset>> = mapOf()
        var alphabetPositionActionMap: Map<String, Map<Int, Map<Int, Int>>> = mapOf()
    }

    data class IconCoordinate(val distance: Double, val angle: Double)
}