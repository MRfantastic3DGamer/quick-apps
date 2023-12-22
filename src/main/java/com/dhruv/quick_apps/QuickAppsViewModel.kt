package com.dhruv.quick_apps

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

/**
 * @param onAlphabetSelectionChange it can be used to add haptic feedback
 * @param onAppSelectionChange it can be used to add haptic feedback
 */
class QuickAppsViewModel(
    val onAlphabetSelectionChange:  (alphabet: String, haptic: HapticFeedback)->Unit,
    val onAppSelectionChange:       (action: Int?, haptic: HapticFeedback)->Unit,
    val onActionSelect:             (Int)->Unit,
    val firstCharToActionsMap:      Map<String, List<Int>>,
    val groupNameToActionsMap:      Map<String, List<Int>>,
    var rowHeight:                  Double,
    var distanceBetweenIcons:       Double,
    val sidePadding:                Float,
    val topMinValue:                Float = -50F,
    val leftMinValue:               Float = -880F,
    val startingRowHeight:          Double = 180.0,
): ViewModel() {

            var selectedString      by mutableStateOf("")
    private var touchPosition       by mutableStateOf(Offset.Zero)

    private var refA                by mutableStateOf(Offset.Zero)
    private var refB                by mutableStateOf(Offset.Zero)
    private var currentAngle        by mutableStateOf(0f)
    private var currentDistance     by mutableStateOf(0f)

    private var currentRow          by mutableStateOf(0)
    private var currentColumn       by mutableStateOf(0)

            var currentAction       by mutableStateOf<Int?>(null)

            var selectionMode       by mutableStateOf(SelectionMode.NonActive)
            var triggerMode         by mutableStateOf(TriggerMode.FirstCharacter)
            var currentActionOffset by mutableStateOf(Offset.Zero)



    // region trigger functions

    fun onTap(){
        // TODO:
    }

    fun onDragStart(offset: Offset) {
        touchPosition = offset
        selectionMode = SelectionMode.TriggerGestureSelect
        if (offset.y > triggerSize.height/2)
            draggedFromTopHalf()
        else
            draggedFromBottomHalf()
    }

    fun draggedFromTopHalf(){
        // TODO: change theme
        triggerMode = TriggerMode.FirstCharacter
    }

    fun draggedFromBottomHalf(){
        // TODO: change theme
        triggerMode = TriggerMode.FirstCharacter
//        triggerMode = TriggerMode.GroupName
    }

    fun onDrag(change: Offset) {

        // region internal functions
        fun getCurrentAlphabet(position: Float, actionsMap: Map<String, List<Int>>): String {
            if (actionsMap.isEmpty()) return ""
            val delta = triggerSize.height / actionsMap.size
            val index = (position / delta).toInt()
            val currIndex = clamp(index, 0, firstAlphabetToActionsOffsets.size-1)
            val curr = actionsMap.keys.toList()[currIndex]
            if (curr != selectedString){
                onAlphabetSelectionChange(curr, haptic)

                refA = Offset( -(sidePadding + 1),  getSelectedTriggerYOffset)
                refB = Offset( - sidePadding     ,  getSelectedTriggerYOffset)
            }
            return curr
        }
        fun getCurrentAction(): Int? {
            currentAngle = -(calculateAngle(refA, refB, touchPosition) - 270)
            currentDistance = calculateDistance(refB, touchPosition) - (startingRowHeight - rowHeight).toFloat()

            currentRow = clamp((currentDistance/rowHeight).toInt(), 0, Int.MAX_VALUE)
            val deltaAngle = calculateAngleOnCircle(currentRow * rowHeight + startingRowHeight, distanceBetweenIcons)
            currentColumn = (currentAngle/deltaAngle).toInt()
            var nextAction: Int? = null
            if(currentRowColumnToIndexMap[selectedString] == null) nextAction = null
            else if (currentRowColumnToIndexMap[selectedString]!![currentRow] == null) nextAction = null
            else if (currentRowColumnToIndexMap[selectedString]!![currentRow]!![currentColumn] == null) nextAction = null
            else nextAction = currentAllActions[currentRowColumnToIndexMap[selectedString]!![currentRow]!![currentColumn]!!]
            if (currentAction != nextAction){
                onAppSelectionChange(currentAction, haptic)
            }
            return nextAction
        }
        // endregion

        touchPosition += change
        when (selectionMode) {
            SelectionMode.NonActive -> {}
            SelectionMode.TriggerGestureSelect -> {
                if(touchPosition.x < toAppSelectX && change.y > change.x){
                    selectionMode = SelectionMode.AppGestureSelect
                }
                selectedString = getCurrentAlphabet(touchPosition.y, currentActionsMap)
            }
            SelectionMode.AppGestureSelect -> {

                if(touchPosition.x > toCharSelectX){
                    selectionMode = SelectionMode.TriggerGestureSelect
                }
                currentAction = getCurrentAction()
            }
            else -> {}
        }
    }

    fun onDragStop(){
        if(currentAction != null){
            onActionSelect(currentAction as Int)
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

    // endregion

    suspend fun handleIconsPositioningCalculations(topMinValue: Float = -50F, leftMinValue: Float = -880F){
        if(!dirty) return

        // region internal functions definition
        fun generateAlphabetYOffsets(actionsMap: Map<String, List<Int>>): Map<String, Float> {
            val map = mutableMapOf<String,Float>()

            val delta = triggerSize.height/actionsMap.size
            var curr = 0F
            for ((s, _) in actionsMap) {
                map[s] = curr - delta/2
                curr += delta
            }
            return map.toMap()
        }

        /**
         * generates all possible coordinates for icons for [0,0] as its center
         */
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

        /**
         * maps all the actions from external maps into internal list
         */
        fun generateAllActions(){
            val actionsL = mutableListOf<Int>()
            for ((_, actions) in firstCharToActionsMap) {
                for (action in actions) {
                    actionsL.add(action)
                }
            }
            allActions = actionsL.toList()
            actionsL.clear()
            for ((_, actions) in groupNameToActionsMap) {
                for (action in actions) {
                    actionsL.add(action)
                }
            }
            allGroupedActions = actionsL.toList()
        }

        /**
         * @param actionsIndexesMap internal indexes for string
         * @param actionsPositionMap internal index for string for row for column
         * @param maxAppsRegionRadius max radius for string (for app region visual)
         */
        data class GenerateIconMapResult(
            val actionsIndexesMap: Map<String, List<Int>>,
            val actionsPositionMap: Map<String, Map<Int, Map<Int, Int>>>,
            val maxAppsRegionRadius: Map<String, Float>
        )
        fun generateIconMap(
            bottomMaxValue  : Float = 1400F,
            topMinValue     : Float = -50F,
            leftMinValue    : Float = -880F,
            actionMap       : Map<String, List<Int>>,
            yOffsets        : Map<String, Float>,
        ): GenerateIconMapResult {
            val iconsM = mutableMapOf<String,List<Int>>();
            val positionToActionIndexMap: MutableMap<String, MutableMap<Int, MutableMap<Int, Int>>> = mutableMapOf()
            val maxRadius = mutableMapOf<String, Float>()
            var c_i: Int
            var action_i = 0
            for ((s, actions) in actionMap){
                c_i = 0
                val origin = yOffsets[s] as Float
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
            return GenerateIconMapResult(iconsM, positionToActionIndexMap, maxRadius)
        }

        /**
         * create cash of offsets from coordinates
         */
        fun generateActionOffsets(actionsMap: Map<String, List<Int>>): Map<String, List<Offset>> {
            val list = mutableMapOf<String,List<Offset>>()

            for ((s, icons) in actionsMap){
                val l = mutableListOf<Offset>()
                for (p in icons){
                    val cpos = getPositionOnCircle(allIconCoordinates[p])
                    val finalPos = -cpos
                    l.add( finalPos )
                }
                list[s] = l.toList()
            }
            return list.toMap()
        }
        // endregion


        // TODO: these two can be done parallelly
        generateAllActions()
        generateIconCoordinates(startingRadius = startingRowHeight, radiusDiff = rowHeight, iconDistance = distanceBetweenIcons)


        // first generate y-offsets for trigger then create all the rest of required mapping
        if (firstCharToActionsMap.isNotEmpty()){
            firstAlphabetYOffsets = generateAlphabetYOffsets(firstCharToActionsMap)


            val generateIconMapRes1 = generateIconMap(
                bottomMaxValue = triggerSize.height.toFloat() - rowHeight.toFloat(),
                topMinValue= topMinValue,
                leftMinValue = leftMinValue,
                actionMap = firstCharToActionsMap,
                yOffsets = firstAlphabetYOffsets
            )
            firstAlphabetToActionsIndexes = generateIconMapRes1.actionsIndexesMap
            firstAlphabetToActionsPositionMap = generateIconMapRes1.actionsPositionMap
            firstAlphabetToMaxAppsRegionRadius = generateIconMapRes1.maxAppsRegionRadius
        }
        if (groupNameToActionsMap.isNotEmpty()) {
            groupNameYOffsets = generateAlphabetYOffsets(groupNameToActionsMap)


            val generateIconMapRes2 = generateIconMap(
                bottomMaxValue = triggerSize.height.toFloat() - rowHeight.toFloat(),
                topMinValue = topMinValue,
                leftMinValue = leftMinValue,
                actionMap = firstCharToActionsMap,
                yOffsets = firstAlphabetYOffsets
            )
            groupNameToActionsIndexes = generateIconMapRes2.actionsIndexesMap
            groupNameToActionsPositionMap = generateIconMapRes2.actionsPositionMap
            groupNameToMaxAppsRegionRadius = generateIconMapRes2.maxAppsRegionRadius
        }

        firstAlphabetToActionsOffsets = generateActionOffsets(firstAlphabetToActionsIndexes)
        groupNameToActionsOffsets = generateActionOffsets(groupNameToActionsIndexes)
        dirty = false
    }

    // region public getters
    val currentTriggerYOffsets: Map<String, Float>
        get() = when (triggerMode) {
            TriggerMode.FirstCharacter -> firstAlphabetYOffsets
            TriggerMode.GroupName -> groupNameYOffsets
        }
    val getSelectedTriggerYOffset: Float
        get() = currentTriggerYOffsets[selectedString] ?: 0f
    val currentActionsMap: Map<String, List<Int>>
        get() = when (triggerMode) {
            TriggerMode.FirstCharacter -> firstCharToActionsMap
            TriggerMode.GroupName -> groupNameToActionsMap
        }
    val currentRowColumnToIndexMap: Map<String, Map<Int, Map<Int, Int>>>
        get() = when (triggerMode) {
            TriggerMode.FirstCharacter -> firstAlphabetToActionsPositionMap
            TriggerMode.GroupName -> groupNameToActionsPositionMap
        }
    val currentAllActions: List<Int>
        get() = when (triggerMode) {
            TriggerMode.FirstCharacter -> allActions
            TriggerMode.GroupName -> allGroupedActions
        }
    val currentActionsOffsets: Map<String, List<Offset>>
        get() = when (triggerMode) {
            TriggerMode.FirstCharacter -> firstAlphabetToActionsOffsets
            TriggerMode.GroupName -> groupNameToActionsOffsets
        }
    val currentMaxAppsRegionRadius: Map<String, Float>
        get() = when (triggerMode) {
            TriggerMode.FirstCharacter -> firstAlphabetToMaxAppsRegionRadius
            TriggerMode.GroupName -> groupNameToMaxAppsRegionRadius
        }
    val getIconsOffsetsChange: List<Offset>
        get() = currentActionsOffsets[selectedString] ?: listOf()
    val currentActions : List<Int>
        get() = currentActionsMap[selectedString] ?: listOf()
    val getTriggerOffset : IntOffset
        get() = triggerOffset.round()
    val getTriggerSize : IntSize
        get() = triggerSize
    val getAlphabetAppsMaxRadius : Float
        get() = currentMaxAppsRegionRadius[selectedString] ?: 0f
    val getGlobalTouchPosition: Offset
        get() = triggerOffset + touchPosition

    fun getGlobalResponsiveBubblePosition(iconSize: Float): Offset {
        return when (selectionMode) {
            SelectionMode.NonActive -> {
                Offset.Zero
            }

            SelectionMode.TriggerGestureSelect -> {
                triggerOffset + Offset(-100f, getSelectedTriggerYOffset)
            }

            SelectionMode.AppGestureSelect -> {
                if (currentAction!=null) (currentActionOffset + triggerOffset + Offset(iconSize, iconSize)) else getGlobalTouchPosition
            }

            else -> {Offset.Zero}
        }
    }
    // endregion

    fun markDirty(){
        dirty = true
    }

    fun setHapticFeedback(h: HapticFeedback){
        haptic = h
    }

    companion object{
        const       val toAppSelectX = -10
        const       val toCharSelectX = 0
                    var triggerSize                                                             = IntSize(0,0)
                    var triggerOffset                                                           = Offset(0f,0f)

                    var firstAlphabetYOffsets: Map<String,Float>                                = mapOf()
                    var groupNameYOffsets: Map<String,Float>                                    = mapOf()
                    var allIconCoordinates: List<IconCoordinate>                                = listOf()
                    var indexToRowAndColumn: List<List<Int>>                                    = listOf()
                    var allActions: List<Int>                                                   = listOf()
                    var allGroupedActions: List<Int>                                            = listOf()
                    var firstAlphabetToActionsIndexes: Map<String,List<Int>>                    = mapOf()
                    var groupNameToActionsIndexes: Map<String,List<Int>>                        = mapOf()
                    var firstAlphabetToActionsOffsets: Map<String,List<Offset>>                 = mapOf()
                    var groupNameToActionsOffsets: Map<String,List<Offset>>                     = mapOf()
                    var firstAlphabetToMaxAppsRegionRadius: Map<String, Float>                  = mapOf()
                    var groupNameToMaxAppsRegionRadius: Map<String, Float>                      = mapOf()
                    var firstAlphabetToActionsPositionMap: Map<String, Map<Int, Map<Int, Int>>> = mapOf()
                    var groupNameToActionsPositionMap: Map<String, Map<Int, Map<Int, Int>>>     = mapOf()

        lateinit    var haptic: HapticFeedback

        private     var dirty = true
    }

    data class IconCoordinate(val distance: Double, val angle: Double)
}