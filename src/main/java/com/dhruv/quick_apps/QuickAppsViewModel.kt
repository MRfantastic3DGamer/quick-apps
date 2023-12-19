package com.dhruv.quick_apps

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * @param onAlphabetSelectionChange it can be used to add haptic feedback
 * @param onAppSelectionChange it can be used to add haptic feedback
 */
class QuickAppsViewModel(
    val onAlphabetSelectionChange: (alphabet: String, haptic: HapticFeedback)->Unit,
    val onAppSelectionChange: (action: Action?, haptic: HapticFeedback)->Unit,
    val firstCharToActionsMap: Map<String, List<Action>>,
    val groupNameToActionsMap: Map<String, List<Action>>,
    var rowHeight: Double,
    var distanceBetweenIcons: Double,
    val sidePadding: Float,
    val topMinValue: Float = -50F,
    val leftMinValue: Float = -880F,
    val startingRowHeight: Double = 180.0
): ViewModel() {
    var currentStringToActionsMap by mutableStateOf(mapOf<String, List<Action>>())

    var touchPosition by mutableStateOf(Offset.Zero)
    var selectedString by mutableStateOf("")

    private var refA by mutableStateOf(Offset.Zero)
    private var refB by mutableStateOf(Offset.Zero)
    var currentAngle by mutableStateOf(0f)
    var currentDistance by mutableStateOf(0f)

    var currentRow by mutableStateOf(0)
    var currentColumn by mutableStateOf(0)
    var currentAction by mutableStateOf<Action?>(null)


    var selectionMode by mutableStateOf(SelectionMode.NonActive)

    var currentActionOfset by mutableStateOf(Offset.Zero)


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
                if(touchPosition.x < toAppSelectX && change.y > change.x){
                    selectionMode = SelectionMode.AppSelect
                    refA = Offset( -(sidePadding + 1),alphabetYOffsets[selectedString] as Float)
                    refB = Offset( -sidePadding,alphabetYOffsets[selectedString] as Float)
                }
                selectedString = getCurrentAlphabet(touchPosition.y)
            }
            SelectionMode.AppSelect -> {

                if(touchPosition.x > toCharSelectX){
                    selectionMode = SelectionMode.CharSelect
                }
                currentAngle = -(calculateAngle(refA, refB, touchPosition) - 270)
                currentDistance = calculateDistance(refB, touchPosition) - (startingRowHeight - rowHeight).toFloat()

                currentRow = clamp((currentDistance/rowHeight).toInt(), 0, Int.MAX_VALUE)
                val deltaAngle = calculateAngleOnCircle(currentRow * rowHeight + startingRowHeight, distanceBetweenIcons)
                currentColumn = (currentAngle/deltaAngle).toInt()
                var nextAction: Action? = null
                if(firstAlphabetToActionsPositionMap[selectedString] == null) nextAction = null
                else if (firstAlphabetToActionsPositionMap[selectedString]!![currentRow] == null) nextAction = null
                else if (firstAlphabetToActionsPositionMap[selectedString]!![currentRow]!![currentColumn] == null) nextAction = null
                else nextAction = allActions[firstAlphabetToActionsPositionMap[selectedString]!![currentRow]!![currentColumn]!!]
                if (currentAction != nextAction){
                    currentAction = nextAction
                    onAppSelectionChange(currentAction, haptic)
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
        selectedString = ""
        currentDistance = 0f
        currentAngle    = 0f
        currentRow = 0
        currentColumn = 0
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onTriggerGloballyPositioned(layoutCoordinates: LayoutCoordinates) {
        triggerSize = layoutCoordinates.size
        triggerOffset = layoutCoordinates.positionInRoot()

        GlobalScope.launch(Dispatchers.IO) {
            handleIconsPositioningCalculations(leftMinValue = leftMinValue, topMinValue = topMinValue)
        }
    }

    private fun getCurrentAlphabet(position: Float): String {
        val delta = triggerSize.height / currentStringToActionsMap.size
        val index = (position / delta).toInt()
        val currIndex = clamp(index, 0, firstAlphabetToActionsOffsets.size-1)
        val curr = currentStringToActionsMap.keys.toList()[currIndex]
        if (curr != selectedString){
            onAlphabetSelectionChange(curr, haptic)
        }
        return curr
    }

    private fun getPositionOnCircle(coordinate: IconCoordinate): Offset {
        return getPositionOnCircle(coordinate.distance, -coordinate.angle)
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

    suspend fun handleIconsPositioningCalculations(topMinValue: Float = -50F, leftMinValue: Float = -880F){
        if(!dirty) return

        fun generateAlphabetYOffsets(){
            val map = mutableMapOf<String,Float>()
            val delta = triggerSize.height/currentStringToActionsMap.size
            var curr = 0F
            for ((s, _) in currentStringToActionsMap) {
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
            for ((_, actions) in currentStringToActionsMap) {
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
            for ((s, actions) in currentStringToActionsMap){
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
                maxRadius[s] = allIconCoordinates[iconsL.last()].distance.toFloat() + rowHeight.toFloat()
            }
            firstAlphabetToActionsIndexes = iconsM
            firstAlphabetToActionsPositionMap = positionToActionIndexMap.toMap()
            firstAlphabetToMaxAppsRegionRadius = maxRadius.toMap()
        }

        fun generateActionPositions(){
            val list = mutableMapOf<String,List<Offset>>()

            for ((s, icons) in firstAlphabetToActionsIndexes){
                val l = mutableListOf<Offset>()
                for (p in icons){
                    val cpos = getPositionOnCircle(allIconCoordinates[p])
                    val finalPos = -cpos
                    l.add( finalPos )
                }
                list[s] = l.toList()
            }
            firstAlphabetToActionsOffsets = list.toMap()
        }

        generateAlphabetYOffsets()
        generateAllActions()

        generateIconCoordinates(startingRadius = startingRowHeight, radiusDiff = rowHeight, iconDistance = distanceBetweenIcons)
        generateIconMap(bottomMaxValue = triggerSize.height.toFloat() - rowHeight.toFloat(), topMinValue= topMinValue, leftMinValue = leftMinValue)
        generateActionPositions()

        println("complete : $firstAlphabetToActionsOffsets")

        dirty = false
    }

    val getSelectedStringYOffset: Float
        get() = alphabetYOffsets[selectedString] ?: 0f
    val getAlphabetYOffsets: Map<String,Float>
        get() = alphabetYOffsets
    val getIconsOffsetsChange: List<Offset>
        get() = firstAlphabetToActionsOffsets[selectedString] ?: listOf()
    val getActionsForAlphabet : List<Action>
        get() = currentStringToActionsMap[selectedString] ?: listOf()
    val getTriggerOffset : IntOffset
        get() = triggerOffset.round()
    val getTriggerSize : IntSize
        get() = triggerSize
    val getAlphabetAppsMaxRadius : Float
        get() = firstAlphabetToMaxAppsRegionRadius[selectedString] ?: 0f
    val getGlobalTouchPosition: Offset
        get() = triggerOffset + touchPosition
    fun getGlobalResponsiveBubblePosition(iconSize: Float): Offset {
        return when (selectionMode) {
            SelectionMode.NonActive -> {
                Offset.Zero
            }

            SelectionMode.CharSelect -> {
                triggerOffset + Offset(-100f, getSelectedStringYOffset)
            }

            SelectionMode.AppSelect -> {
                if (currentAction!=null) (currentActionOfset + triggerOffset + Offset(iconSize, iconSize)) else getGlobalTouchPosition
            }
        }
    }
    val getHapticFeedback: HapticFeedback
        get() = haptic

    fun markDirty(){
        dirty = true
    }

    fun setHapticFeedback(h: HapticFeedback){
        haptic = h
    }

    companion object{
        val toAppSelectX = -10
        val toCharSelectX = 0
        var triggerSize = IntSize(0,0)
        var triggerOffset = Offset(0f,0f)

        var alphabetYOffsets: Map<String,Float> = mapOf()
        var allIconCoordinates: List<IconCoordinate> = listOf()
        var indexToRowAndColumn: List<List<Int>> = listOf()
        var allActions: List<Action> = listOf()
        var firstAlphabetToActionsIndexes: Map<String,List<Int>> = mapOf()
        var groupNameToActionsIndexes: Map<String,List<Int>> = mapOf()
        var firstAlphabetToActionsOffsets: Map<String,List<Offset>> = mapOf()
        var groupNameToActionsOffsets: Map<String,List<Offset>> = mapOf()
        var firstAlphabetToMaxAppsRegionRadius: Map<String, Float> = mapOf()
        var groupNameToMaxAppsRegionRadius: Map<String, Float> = mapOf()
        var firstAlphabetToActionsPositionMap: Map<String, Map<Int, Map<Int, Int>>> = mapOf()
        var groupNameToActionsPositionMap: Map<String, Map<Int, Map<Int, Int>>> = mapOf()

        lateinit var haptic: HapticFeedback

        var dirty = true
    }

    data class IconCoordinate(val distance: Double, val angle: Double)
}