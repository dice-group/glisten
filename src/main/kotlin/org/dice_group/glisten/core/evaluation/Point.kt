package org.dice_group.glisten.core.evaluation

/**
 * A simple Point class using x and y coordinates
 *
 * @param x The x coordinate, default is 0.0
 * @param y The y coordinate, default is 0.0
 */
class Point(var x: Double =0.0, var y: Double =0.0) {


    /**
     * Sets the new location of the Point
     *
     * This is a convenience function for
     *
     * ```kotlin
     * point.x = X
     * point.y = Y
     * ```
     */
    fun setLocation(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("[ x : ").append(x).append(", y : ").append(y).append(" ]")
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if(other is Point){
            return other.x == other.x &&
                    other.y == other.y
        }
        return false
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}