package com.dhruv.quick_apps

import androidx.compose.ui.geometry.Offset
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

fun calculateAngle(a: Offset, b: Offset, c: Offset): Float {
    val ab: Double = sqrt((b.x - a.x).toDouble().pow(2.0) + (b.y - a.y).toDouble().pow(2.0))
    val bc: Double = sqrt((c.x - b.x).toDouble().pow(2.0) + (c.y - b.y).toDouble().pow(2.0))
    val ac: Double = sqrt((c.x - a.x).toDouble().pow(2.0) + (c.y - a.y).toDouble().pow(2.0))
    val ratio : Double = (ab * ab + ac * ac - bc * bc) /( 2 * ac * ab)
    var degree = acos(ratio) *(180/Math.PI)
    if(c.y > b.y) degree = 360 - degree
    return degree.toFloat()
}

fun calculateDistance(a: Offset, b: Offset): Float{
    val X = b.x - a.x
    val Y = b.y - a.y
    return sqrt((X*X) + (Y*Y))
}

fun calculateAngleOnCircle(radius: Double, distance: Double): Double {
    if (radius <= 0.0 || distance <= 0.0) {
        throw IllegalArgumentException("Both radius and distance must be positive values , radius:${radius}, distance:${distance}")
    }
    val angleInRadians = distance / radius
    val angleInDegrees = Math.toDegrees(angleInRadians)
    return angleInDegrees
}