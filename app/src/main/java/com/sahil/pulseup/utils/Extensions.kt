package com.sahil.pulseup.utils

import android.content.Context
import android.widget.Toast

/**
 * Extension functions for common operations
 */

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showToast(messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageResId, duration).show()
}

/**
 * Extension function to safely get color from resources
 */
fun Context.getColorSafely(colorResId: Int, fallbackColor: Int = android.R.color.black): Int {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getColor(colorResId)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(colorResId)
        }
    } catch (e: Exception) {
        fallbackColor
    }
}

/**
 * Extension function to check if string is not null and not empty
 */
fun String?.isNotNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

/**
 * Extension function to safely convert string to int
 */
fun String?.toIntOrZero(): Int {
    return this?.toIntOrNull() ?: 0
}

/**
 * Extension function to safely convert string to float
 */
fun String?.toFloatOrZero(): Float {
    return this?.toFloatOrNull() ?: 0f
}
